/**
 * iCalendar (.ics) 生成器
 *
 * 这个文件是整个网页版的核心，只用纯函数，不碰界面，所以可以直接在 Node 里跑测试。
 *
 * 生成的每条日程长这样：
 *   BEGIN:VEVENT
 *   DTSTART;VALUE=DATE:20261114     ← 全天事件
 *   RRULE:FREQ=YEARLY               ← 每年重复
 *   BEGIN:VALARM
 *   TRIGGER:-PT15H                  ← 当天 09:00 提醒（见下方说明）
 *   END:VALARM
 *   END:VEVENT
 */

const encoder = new TextEncoder();
const DAY_MS = 24 * 60 * 60 * 1000;

/* ------------------------------------------------------------------ *
 * 时间工具
 * ------------------------------------------------------------------ */

/** 当天零点，用来避免时分秒影响天数计算 */
export function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

/**
 * 把 (月, 日) 落到指定年份。2 月 29 日在平年会被压到 28 日。
 * 注意这里的 month 是 1~12（人的直觉），不是 JS 的 0~11。
 */
function safeDate(year, month, day) {
  const maxDay = new Date(year, month, 0).getDate();
  return new Date(year, month - 1, Math.min(day, maxDay));
}

/** 下一次生日（今年还没过就今年，过了就明年） */
export function nextOccurrence(month, day, today = new Date()) {
  const base = startOfDay(today);
  let date = safeDate(base.getFullYear(), month, day);
  if (date < base) date = safeDate(base.getFullYear() + 1, month, day);
  return date;
}

/** 距离下一次生日还有几天，今天就是生日返回 0 */
export function daysUntil(month, day, today = new Date()) {
  const base = startOfDay(today);
  return Math.round((nextOccurrence(month, day, today) - base) / DAY_MS);
}

/** "3月18日" */
export function formatMonthDay(month, day) {
  return `${month}月${day}日`;
}

/** 界面上那句倒计时 */
export function countdownText(days) {
  if (days === 0) return '就是今天 🎉';
  if (days === 1) return '明天生日';
  if (days === 2) return '后天生日';
  return `还有 ${days} 天`;
}

/* ------------------------------------------------------------------ *
 * iCalendar 文本规则
 * ------------------------------------------------------------------ */

function pad(n) {
  return String(n).padStart(2, '0');
}

/** 20261114 这种「纯日期」格式，iCalendar 的全天事件必须用这种 */
function dateValue(date) {
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`;
}

/** DTSTAMP 要 UTC 时间：20251114T120000Z */
function utcStamp(date) {
  return (
    `${date.getUTCFullYear()}${pad(date.getUTCMonth() + 1)}${pad(date.getUTCDate())}` +
    `T${pad(date.getUTCHours())}${pad(date.getUTCMinutes())}${pad(date.getUTCSeconds())}Z`
  );
}

/**
 * 转义。iCalendar 里逗号、分号、反斜杠、换行都是「控制字符」，
 * 出现在描述文字里必须转义，否则整个文件解析失败。
 */
export function escapeText(value) {
  return String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/;/g, '\\;')
    .replace(/,/g, '\\,')
    .replace(/\r\n|\r|\n/g, '\\n');
}

/**
 * 折行。规范要求每行不超过 75 个「字节」（不是字符！），超出要用「CRLF + 空格」续行。
 * 中文在 UTF-8 下每个字占 3 字节，所以中文描述很容易超长——必须折，不折 iOS 会拒收。
 */
export function foldLine(line) {
  const bytes = encoder.encode(line);
  if (bytes.length <= 75) return line;

  const parts = [];
  let current = '';
  let size = 0;

  for (const char of line) {
    const charSize = encoder.encode(char).length;
    // 续行开头那个空格也算字节，所以续行只能放 74
    const limit = parts.length === 0 ? 75 : 74;
    if (size + charSize > limit) {
      parts.push(current);
      current = '';
      size = 0;
    }
    current += char;
    size += charSize;
  }
  if (current) parts.push(current);

  return parts.join('\r\n ');
}

/* ------------------------------------------------------------------ *
 * 三个提醒的文案
 * ------------------------------------------------------------------ */

/** 提前几天 */
export const REMINDER_OFFSETS = [7, 3, 0];

function planTitle(offset, friend) {
  if (offset === 7) return `给${friend.name}挑生日礼物 🎁`;
  if (offset === 3) return `准备${friend.name}的生日礼物 🎀`;
  return `${friend.name} 生日快乐 🎂`;
}

function planDescription(offset, friend) {
  const hobby = friend.hobbies?.length ? `\nTA 的爱好：${friend.hobbies.join('、')}` : '';
  const birthday = formatMonthDay(friend.month, friend.day);
  if (offset === 7) {
    return `还有 7 天就是${friend.name}的生日（${birthday}）。今天把礼物挑好、下单，物流才来得及。${hobby}`;
  }
  if (offset === 3) {
    return `还有 3 天！检查${friend.name}的礼物是否到货、要不要包装，想好当天怎么送。${hobby}`;
  }
  return `今天是${friend.name}的生日，记得发祝福。${hobby}`;
}

/* ------------------------------------------------------------------ *
 * 拼装
 * ------------------------------------------------------------------ */

function eventLines(friend, offset, stamp) {
  const today = startOfDay(new Date());
  const birthday = nextOccurrence(friend.month, friend.day, today);

  let date = new Date(birthday.getTime() - offset * DAY_MS);
  // 提醒日期已经过去就顺延到明年，别生成一条过去日期的日程
  if (date < today) date = safeDate(date.getFullYear() + 1, date.getMonth() + 1, date.getDate());

  const nextDay = new Date(date.getTime() + DAY_MS);
  const title = planTitle(offset, friend);
  const description = planDescription(offset, friend);

  return [
    'BEGIN:VEVENT',
    // UID 固定不变，重复导入时系统会「更新」而不是「再插一条」
    `UID:friend-${friend.id}-${offset}@birthday-reminder`,
    `DTSTAMP:${stamp}`,
    `DTSTART;VALUE=DATE:${dateValue(date)}`,
    `DTEND;VALUE=DATE:${dateValue(nextDay)}`,
    `SUMMARY:${escapeText(title)}`,
    `DESCRIPTION:${escapeText(description)}`,
    'TRANSP:TRANSPARENT',
    'RRULE:FREQ=YEARLY',
    'BEGIN:VALARM',
    'ACTION:DISPLAY',
    `DESCRIPTION:${escapeText(title)}`,
    // 全天事件以「当天零点」为基准：提前 15 小时 = 当天 09:00
    'TRIGGER:-PT15H',
    'END:VALARM',
    'END:VEVENT',
  ];
}

/**
 * 生成完整 .ics 文本。
 * @param {Array} friends 好友列表
 * @param {Object} options { now: Date }
 * @returns {string} 带 CRLF 换行的 iCalendar 文本
 */
export function buildIcs(friends, { now = new Date() } = {}) {
  const stamp = utcStamp(now);
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Birthday Reminder//CN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    'X-WR-CALNAME:生日提醒',
  ];

  for (const friend of friends) {
    for (const offset of REMINDER_OFFSETS) {
      lines.push(...eventLines(friend, offset, stamp));
    }
  }

  lines.push('END:VCALENDAR');

  return lines.map(foldLine).join('\r\n') + '\r\n';
}
