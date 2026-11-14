import { buildIcs, daysUntil, formatMonthDay, countdownText } from './ics.js';

/* ================================================================== *
 * 状态
 * ================================================================== */

const STORAGE_KEY = 'birthday-reminder.friends.v1';

/** @type {Array<{id:number,name:string,month:number,day:number,birthYear:number|null,hobbies:string[]}>} */
let friends = loadFriends();

/** 当前视图：'list' 列表 | 'form' 表单 */
let view = 'list';
/** 正在编辑的好友，null 表示新增 */
let editing = null;

/* ================================================================== *
 * 存储
 * ================================================================== */

function loadFriends() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function saveFriends() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(friends));
  } catch (err) {
    toast('保存失败，可能是不允许本地存储');
    console.error(err);
  }
}

/* ================================================================== *
 * 工具
 * ================================================================== */

/** 把用户输入转义后再插进 HTML，防止名字里带 <script> 把页面搞乱 */
function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (ch) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[ch]));
}

const AVATAR_COLORS = ['#E8623C', '#7B8CDE', '#56A08A', '#D98E3A', '#9C6ADE', '#4A9FBF'];

function avatarColor(name) {
  let sum = 0;
  for (const ch of name) sum += ch.codePointAt(0);
  return AVATAR_COLORS[sum % AVATAR_COLORS.length];
}

function sortedFriends() {
  return [...friends].sort((a, b) => daysUntil(a.month, a.day) - daysUntil(b.month, b.day));
}

let toastTimer = null;
function toast(message) {
  let el = document.querySelector('.toast');
  if (!el) {
    el = document.createElement('div');
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = message;
  requestAnimationFrame(() => el.classList.add('show'));
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 2400);
}

/** 触发浏览器下载 */
function downloadFile(filename, text) {
  const blob = new Blob([text], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.rel = 'noopener';
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    URL.revokeObjectURL(url);
    a.remove();
  }, 1500);
}

function safeFilename(text) {
  return String(text).replace(/[\\/:*?"<>|]/g, '').trim() || '好友';
}

/* ================================================================== *
 * 渲染
 * ================================================================== */

const viewEl = document.getElementById('view');
const titleEl = document.getElementById('title');
const backBtn = document.getElementById('backBtn');
const addBtn = document.getElementById('addBtn');

function render() {
  if (view === 'form') {
    titleEl.textContent = editing ? '编辑好友' : '添加好友';
    backBtn.hidden = false;
    addBtn.hidden = true;
    viewEl.innerHTML = formHtml();
    bindForm();
  } else {
    titleEl.textContent = '生日提醒';
    backBtn.hidden = true;
    addBtn.hidden = false;
    viewEl.innerHTML = listHtml();
  }
}

function listHtml() {
  const list = sortedFriends();

  if (list.length === 0) {
    return `
      <div class="empty">
        <div class="emoji">🎂</div>
        <h2>还没有好友</h2>
        <p>添加第一个好友，<br>点一下就能把他的生日写进手机日历，<br>提前一周提醒你买礼物。</p>
        <button class="btn btn-primary" data-action="add">添加好友</button>
      </div>`;
  }

  const cards = list.map((f) => {
    const days = daysUntil(f.month, f.day);
    const badgeClass = days === 0 ? 'badge today' : days <= 7 ? 'badge soon' : 'badge';
    const meta = [
      formatMonthDay(f.month, f.day),
      f.birthYear ? `${new Date().getFullYear() - f.birthYear} 岁` : '',
      f.hobbies.length ? f.hobbies.join(' ') : '',
    ].filter(Boolean).join(' · ');

    return `
      <article class="card${days <= 7 ? ' soon' : ''}">
        <div class="card-main">
          <div class="avatar" style="background:${avatarColor(f.name)}">${esc(f.name.slice(0, 1))}</div>
          <div class="who">
            <div class="name">${esc(f.name)}</div>
            <div class="meta">${esc(meta)}</div>
          </div>
          <span class="${badgeClass}">${esc(countdownText(days))}</span>
        </div>
        <div class="card-actions">
          <button class="primary" data-action="import" data-id="${f.id}">导入到日历</button>
          <button data-action="edit" data-id="${f.id}">编辑</button>
          <button class="danger" data-action="delete" data-id="${f.id}">删除</button>
        </div>
      </article>`;
  }).join('');

  const allButton = list.length > 1
    ? `<div class="toolbar"><button class="btn btn-ghost btn-small" data-action="import-all">全部导入（${list.length} 人）</button></div>`
    : '';

  return `${allButton}<div class="list">${cards}</div>
    <button class="fab" data-action="add" aria-label="添加好友">＋</button>`;
}

function formHtml() {
  const f = editing;
  const month = f?.month ?? new Date().getMonth() + 1;
  const day = f?.day ?? new Date().getDate();
  const hasYear = Boolean(f?.birthYear);
  const year = f?.birthYear ?? new Date().getFullYear() - 25;

  const monthOptions = Array.from({ length: 12 }, (_, i) => i + 1)
    .map((m) => `<option value="${m}"${m === month ? ' selected' : ''}>${m} 月</option>`).join('');
  const dayOptions = Array.from({ length: 31 }, (_, i) => i + 1)
    .map((d) => `<option value="${d}"${d === day ? ' selected' : ''}>${d} 日</option>`).join('');

  return `
    <div class="form">
      <div class="field">
        <label for="fName">名字</label>
        <input id="fName" type="text" placeholder="比如：王小明" value="${esc(f?.name ?? '')}"
               autocomplete="off" enterkeyhint="done">
      </div>

      <div class="field">
        <label>生日</label>
        <div class="row">
          <select id="fMonth">${monthOptions}</select>
          <select id="fDay">${dayOptions}</select>
        </div>
      </div>

      <div class="switch-row">
        <div class="label">
          记录出生年份
          <div class="hint">知道的话可以显示今年过几岁生日</div>
        </div>
        <label class="switch">
          <input type="checkbox" id="fHasYear"${hasYear ? ' checked' : ''}>
          <span class="track"></span>
        </label>
      </div>

      <div class="field" id="yearField"${hasYear ? '' : ' hidden'}>
        <label for="fYear">出生年份</label>
        <input id="fYear" type="number" inputmode="numeric" min="1900" max="${new Date().getFullYear()}"
               value="${year}">
      </div>

      <div class="field">
        <label for="fHobbies">爱好（选填）</label>
        <input id="fHobbies" type="text" placeholder="篮球 摄影 咖啡，用空格或逗号分隔"
               value="${esc(f?.hobbies?.join(' ') ?? '')}">
      </div>

      <button class="btn btn-primary" id="saveBtn" disabled>保存</button>

      <p class="note">
        保存后点「导入到日历」，会下载一个 <code>.ics</code> 文件。
        打开它，系统就会把 3 个日程加进你的日历：提前 7 天买礼物、提前 3 天准备、当天送祝福。
      </p>
    </div>`;
}

/* ================================================================== *
 * 表单交互
 * ================================================================== */

function bindForm() {
  const nameInput = document.getElementById('fName');
  const monthSel = document.getElementById('fMonth');
  const daySel = document.getElementById('fDay');
  const hasYear = document.getElementById('fHasYear');
  const yearField = document.getElementById('yearField');
  const yearInput = document.getElementById('fYear');
  const hobbiesInput = document.getElementById('fHobbies');
  const saveBtn = document.getElementById('saveBtn');

  const syncSaveBtn = () => {
    saveBtn.disabled = nameInput.value.trim().length === 0;
  };
  syncSaveBtn();
  nameInput.addEventListener('input', syncSaveBtn);

  // 2 月只有 28/29 天，切换月份时要调整「日」的最大值
  const syncDays = () => {
    const month = Number(monthSel.value);
    const maxDay = new Date(new Date().getFullYear(), month, 0).getDate();
    const current = Number(daySel.value);

    daySel.innerHTML = Array.from({ length: maxDay }, (_, i) => i + 1)
      .map((d) => `<option value="${d}"${d === Math.min(current, maxDay) ? ' selected' : ''}>${d} 日</option>`)
      .join('');
  };
  monthSel.addEventListener('change', syncDays);

  hasYear.addEventListener('change', () => {
    yearField.hidden = !hasYear.checked;
  });

  saveBtn.addEventListener('click', () => {
    const name = nameInput.value.trim();
    if (!name) return;

    const friend = {
      id: editing?.id ?? Date.now(),
      name,
      month: Number(monthSel.value),
      day: Number(daySel.value),
      birthYear: hasYear.checked ? Number(yearInput.value) || null : null,
      hobbies: splitHobbies(hobbiesInput.value),
    };

    if (editing) {
      friends = friends.map((f) => (f.id === friend.id ? friend : f));
    } else {
      friends.push(friend);
    }
    saveFriends();
    editing = null;
    view = 'list';
    render();
    toast('已保存');
  });

  nameInput.focus({ preventScroll: true });
}

function splitHobbies(raw) {
  return [...new Set(
    String(raw).split(/[\s,，、;；]+/).map((s) => s.trim()).filter(Boolean),
  )];
}

/* ================================================================== *
 * 事件
 * ================================================================== */

viewEl.addEventListener('click', (event) => {
  const btn = event.target.closest('[data-action]');
  if (!btn) return;

  const { action, id } = btn.dataset;

  switch (action) {
    case 'add':
      editing = null;
      view = 'form';
      render();
      break;

    case 'edit': {
      editing = friends.find((f) => f.id === Number(id)) ?? null;
      view = 'form';
      render();
      break;
    }

    case 'delete': {
      const target = friends.find((f) => f.id === Number(id));
      if (!target) break;
      if (confirm(`删除「${target.name}」？\n（已经导入到日历的日程需要去日历 App 里手动删除）`)) {
        friends = friends.filter((f) => f.id !== Number(id));
        saveFriends();
        render();
        toast('已删除');
      }
      break;
    }

    case 'import': {
      const target = friends.find((f) => f.id === Number(id));
      if (!target) break;
      downloadFile(`生日提醒-${safeFilename(target.name)}.ics`, buildIcs([target]));
      showSheet(`已生成「${target.name}」的日历文件`);
      break;
    }

    case 'import-all': {
      if (!friends.length) break;
      downloadFile('生日提醒-全部.ics', buildIcs(sortedFriends()));
      showSheet(`已生成全部 ${friends.length} 位好友的日历文件`);
      break;
    }
  }
});

backBtn.addEventListener('click', () => {
  editing = null;
  view = 'list';
  render();
});

addBtn.addEventListener('click', () => {
  editing = null;
  view = 'form';
  render();
});

/* ================================================================== *
 * 说明弹层
 * ================================================================== */

const sheet = document.getElementById('sheet');
const sheetTitle = document.getElementById('sheetTitle');

function showSheet(title) {
  sheetTitle.textContent = title;
  sheet.hidden = false;
}

document.getElementById('sheetClose').addEventListener('click', () => {
  sheet.hidden = true;
});
sheet.addEventListener('click', (event) => {
  if (event.target === sheet) sheet.hidden = true;
});

/* ================================================================== *
 * 启动
 * ================================================================== */

render();

// 离线可用（第二次打开就不需要网络了）
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(() => {
      /* 离线缓存失败不影响主要功能，静默忽略 */
    });
  });
}
