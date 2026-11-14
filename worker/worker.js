/**
 * 生日提醒 · ICS 投递助手（Cloudflare Worker）
 *
 * 目的只有一个：把网页生成的 .ics 内容，用正确的 Content-Type 返回给 iOS。
 *
 * 为什么需要它：
 *   网页里用 Blob 下载的文件，iOS 会当成 application/octet-stream，
 *   存进「文件」App 后不会和日历建立关联，于是没有「添加到日历」的入口。
 *   iOS 只认 Content-Type: text/calendar，而这个响应头必须由服务器给出。
 *
 * 用法：
 *   https://<你的worker地址>/?d=<base64url 编码的 ics 内容>&n=birthday-1114.ics
 *
 * 隐私：Worker 不存储任何数据，只在内存里解码后原样返回。
 */

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const data = url.searchParams.get('d');

    if (!data) {
      return new Response('缺少参数 d', {
        status: 400,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
    }

    let text;
    try {
      text = decodeBase64Url(data);
    } catch {
      return new Response('参数 d 解码失败', {
        status: 400,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
    }

    // 只接受看起来像 iCalendar 的内容，避免被当成任意文件的中转站
    if (!text.startsWith('BEGIN:VCALENDAR')) {
      return new Response('内容不是有效的日历文件', {
        status: 400,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' },
      });
    }

    // 清洗文件名：只留字母数字下划线点横线，去掉开头的点和横线（防止出现 ../ 之类）
    const rawName = url.searchParams.get('n') || 'birthday.ics';
    let filename = rawName.replace(/[^\w.\-]/g, '').replace(/^[.\-]+/, '');
    if (!filename || !filename.toLowerCase().endsWith('.ics')) {
      filename = (filename || 'birthday') + '.ics';
    }

    return new Response(text, {
      headers: {
        // 关键的一行：iOS 靠它认领日历关联
        'Content-Type': 'text/calendar; charset=utf-8',
        'Content-Disposition': `inline; filename="${filename}"`,
        'Cache-Control': 'no-store',
        'Access-Control-Allow-Origin': '*',
        'X-Content-Type-Options': 'nosniff',
      },
    });
  },
};

/** base64url → UTF-8 字符串 */
function decodeBase64Url(input) {
  const b64 = input.replace(/-/g, '+').replace(/_/g, '/').padEnd(
    input.length + ((4 - (input.length % 4)) % 4),
    '=',
  );
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return new TextDecoder().decode(bytes);
}
