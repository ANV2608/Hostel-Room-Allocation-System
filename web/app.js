'use strict';

// ------------------------------------------------------------------
// State kept in the browser. The Java server owns the real data; this
// is just the latest copy it sent back.
// ------------------------------------------------------------------
let state = null;
let activeTab = 'pending';
let toastTimer = null;

// ------------------------------------------------------------------
// Talking to the Java server
// ------------------------------------------------------------------
async function call(path, body) {
  const options = { headers: { 'X-Requested-With': 'hostel-ui' } };
  if (body !== undefined) {
    options.method = 'POST';
    options.body = new URLSearchParams(body);
  }
  try {
    const response = await fetch('/api' + path, options);
    const data = await response.json();
    if (data.state) {
      state = data.state;
      render();
    }
    showToast(data.message, data.ok);
    return data.ok;
  } catch (err) {
    showToast('Cannot reach the Java server. Is WebApp still running?', false);
    return false;
  }
}

function showToast(message, ok) {
  if (!message) return;
  const el = document.getElementById('toast');
  el.textContent = message;
  el.className = 'show ' + (ok ? 'ok' : 'err');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.className = ''; }, 4500);
}

// ------------------------------------------------------------------
// Helpers
// ------------------------------------------------------------------
// Everything that came from the user is escaped before going into HTML
function esc(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function pill(type) {
  if (!type) return '-';
  return '<span class="pill ' + esc(String(type).toLowerCase()) + '">' + esc(type) + '</span>';
}

function km(value) {
  return value < 0 ? '-' : esc(value) + ' km';
}

// Round avatar with the student's initials; the colour comes from the name
function avatar(name) {
  const words = String(name).trim().split(/\s+/).filter(Boolean);
  const letters = words.length === 0 ? '?'
    : (words[0][0] + (words.length > 1 ? words[words.length - 1][0] : '')).toUpperCase();
  let hash = 0;
  for (const ch of String(name)) hash = (hash * 31 + ch.charCodeAt(0)) % 360;
  return '<span class="av" style="--h:' + hash + '">' + esc(letters) + '</span>';
}

function who(name) {
  return '<span class="who">' + avatar(name) + esc(name) + '</span>';
}

function year(value) {
  const y = Number(value);
  const cls = y >= 1 && y <= 4 ? ' y' + y : '';
  return '<span class="yr' + cls + '">Y' + esc(value) + '</span>';
}

// ------------------------------------------------------------------
// Tab definitions: which list to show and which columns it has
// ------------------------------------------------------------------
const TABS = [
  {
    key: 'pending',
    label: 'Pending',
    empty: 'No pending requests. Add one, or click "Load sample data".',
    rows: s => s.pending,
    cols: [
      ['ID', r => esc(r.id)],
      ['Student', r => who(r.name)],
      ['Year', r => year(r.year)],
      ['Distance', r => km(r.distanceKm)],
      ['Wants', r => pill(r.wants)]
    ]
  },
  {
    key: 'assigned',
    label: 'Assigned',
    empty: 'Nobody is assigned yet. Click "Run allocation" to assign rooms.',
    rows: s => s.assigned,
    cols: [
      ['ID', r => esc(r.id)],
      ['Student', r => who(r.name)],
      ['Year', r => year(r.year)],
      ['Distance', r => km(r.distanceKm)],
      ['Room', r => '<strong>' + esc(r.room) + '</strong>'],
      ['Type', r => pill(r.roomType)],
      ['', r => '<button class="btn mini" data-checkout="' + esc(r.id) + '">Check out</button>']
    ]
  },
  {
    key: 'waitlist',
    label: 'Waitlist',
    empty: 'The waitlist is empty.',
    rows: s => s.waitlist,
    cols: [
      ['Queue', r => '<span class="rank">#' + esc(r.rank) + '</span>'],
      ['ID', r => esc(r.id)],
      ['Student', r => who(r.name)],
      ['Year', r => year(r.year)],
      ['Distance', r => km(r.distanceKm)],
      ['Waiting for', r => pill(r.wants)]
    ]
  },
  {
    key: 'rejected',
    label: 'Rejected',
    empty: 'No rejected requests.',
    rows: s => s.rejected,
    cols: [
      ['ID', r => esc(r.id)],
      ['Student', r => who(r.name)],
      ['Reason', r => '<span class="reason">' + esc(r.remark) + '</span>']
    ]
  },
  {
    key: 'vacated',
    label: 'Checked out',
    empty: 'Nobody has checked out yet.',
    rows: s => s.vacated,
    cols: [
      ['ID', r => esc(r.id)],
      ['Student', r => who(r.name)],
      ['Note', r => esc(r.remark)]
    ]
  },
  {
    key: 'log',
    label: 'Event log',
    empty: 'No events yet.',
    rows: s => s.log.map((text, i) => ({ n: i + 1, text: text })),
    cols: [
      ['#', r => esc(r.n)],
      ['Event', r => '<span class="mono">' + esc(r.text) + '</span>']
    ]
  }
];

// ------------------------------------------------------------------
// Rendering
// ------------------------------------------------------------------
function render() {
  if (!state) return;
  renderStats();
  renderRooms();
  renderTabs();
}

function renderStats() {
  const s = state.summary;
  const items = [
    ['pending', s.pending, 'Pending'],
    ['assigned', s.assigned, 'Assigned'],
    ['waitlisted', s.waitlisted, 'Waitlisted'],
    ['rejected', s.rejected, 'Rejected'],
    ['free', s.freeBeds + ' / ' + s.totalBeds, 'Free beds']
  ];
  document.getElementById('stats').innerHTML = items.map(i =>
    '<div class="stat ' + i[0] + '"><div class="num">' + esc(i[1]) +
    '</div><div class="lbl">' + i[2] + '</div></div>'
  ).join('');
}

function renderRooms() {
  const box = document.getElementById('rooms');
  if (state.rooms.length === 0) {
    box.innerHTML = '<p class="empty">No rooms yet. Add one, or click "Load sample data".</p>';
    return;
  }
  box.innerHTML = state.rooms.map(room => {
    let beds = '';
    for (let i = 0; i < room.capacity; i++) {
      const person = room.occupants[i];
      beds += person
        ? '<span class="bed on" title="' + esc(person.name) + '"></span>'
        : '<span class="bed" title="Free bed"></span>';
    }
    const people = room.occupants.length
      ? '<ul>' + room.occupants.map(p =>
          '<li>' + avatar(p.name) + '<span>' + esc(p.name) + '</span></li>').join('') + '</ul>'
      : '<ul><li class="empty">Free</li></ul>';
    return '<div class="room ' + esc(room.type.toLowerCase()) + '">' +
      '<div class="room-head"><strong>' + esc(room.roomNumber) + '</strong>' + pill(room.type) + '</div>' +
      '<div class="beds">' + beds + '</div>' +
      people +
      '<div class="meta">Rs.' + esc(room.rent) + '/month &middot; ' + esc(room.facilities) + '</div>' +
      '</div>';
  }).join('');
}

function renderTabs() {
  const tabsBox = document.getElementById('tabs');
  tabsBox.innerHTML = TABS.map(tab => {
    const count = tab.rows(state).length;
    return '<button class="tab' + (tab.key === activeTab ? ' active' : '') +
      '" data-tab="' + tab.key + '">' + tab.label +
      '<span class="count">' + count + '</span></button>';
  }).join('');

  const tab = TABS.find(t => t.key === activeTab) || TABS[0];
  const rows = tab.rows(state);
  const table = document.getElementById('table');

  if (rows.length === 0) {
    table.innerHTML = '<p class="empty">' + tab.empty + '</p>';
    return;
  }
  const head = '<tr>' + tab.cols.map(c => '<th>' + c[0] + '</th>').join('') + '</tr>';
  const body = rows.map(r =>
    '<tr>' + tab.cols.map(c => '<td>' + c[1](r) + '</td>').join('') + '</tr>'
  ).join('');
  table.innerHTML = '<table><thead>' + head + '</thead><tbody>' + body + '</tbody></table>';
}

// ------------------------------------------------------------------
// Wiring up the page
// ------------------------------------------------------------------
function wireForm(formId, path, after) {
  const form = document.getElementById(formId);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const ok = await call(path, Object.fromEntries(new FormData(form)));
    if (ok) {
      form.reset();
      if (after) after();
    }
  });
}

function init() {
  wireForm('room-form', '/room');
  wireForm('request-form', '/request', () => { activeTab = 'pending'; renderTabs(); });
  wireForm('checkout-form', '/checkout');

  document.getElementById('btn-sample').addEventListener('click', async () => {
    if (await call('/load-sample', {})) { activeTab = 'pending'; renderTabs(); }
  });
  document.getElementById('btn-process').addEventListener('click', async () => {
    if (await call('/process', {})) { activeTab = 'assigned'; renderTabs(); }
  });
  document.getElementById('btn-save').addEventListener('click', () => call('/save-report', {}));
  document.getElementById('btn-reset').addEventListener('click', () => {
    if (confirm('Clear all rooms, requests and results?')) call('/reset', {});
  });

  // Tab buttons (event delegation)
  document.getElementById('tabs').addEventListener('click', event => {
    const button = event.target.closest('[data-tab]');
    if (!button) return;
    activeTab = button.dataset.tab;
    renderTabs();
  });

  // "Check out" buttons inside the Assigned table
  document.getElementById('table').addEventListener('click', event => {
    const button = event.target.closest('[data-checkout]');
    if (button) call('/checkout', { studentId: button.dataset.checkout });
  });

  call('/state');
}

init();
