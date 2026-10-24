
"use strict";

let state = null;
let activeTab = "pending";

// ================================================================
// HELPERS
// ================================================================

function esc(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function pill(type) {
  if (!type) return "-";
  return `<span class="pill ${esc(String(type).toLowerCase())}">${esc(type)}</span>`;
}

function km(value) {
  const n = Number(value);
  return !Number.isFinite(n) || n < 0 ? "-" : esc(n) + " km";
}

function avatar(name) {
  const words = String(name || "").trim().split(/\s+/).filter(Boolean);
  const letters = words.length
    ? (words[0][0] + (words.length > 1 ? words[words.length - 1][0] : "")).toUpperCase()
    : "?";

  let hash = 0;
  for (const ch of String(name || "")) {
    hash = (hash * 31 + ch.charCodeAt(0)) % 360;
  }

  return `<span class="av" style="--h:${hash}">${esc(letters)}</span>`;
}

function who(name, id) {
  return `<span class="who">${avatar(name)}
    <span>${esc(name || "Unknown student")}
      ${id ? `<small>${esc(id)}</small>` : ""}
    </span>
  </span>`;
}

function gender(value) {
  if (value !== "MALE" && value !== "FEMALE") return "-";
  return value === "MALE"
    ? '<span class="gd male">♂ Male</span>'
    : '<span class="gd female">♀ Female</span>';
}

function year(value) {
  const y = Number(value);
  const cls = y >= 1 && y <= 4 ? " y" + y : "";
  return `<span class="yr${cls}">Y${esc(value)}</span>`;
}

function showToast(message, error = false) {
  const toast = document.getElementById("toast");

  if (!toast) {
    if (message) alert(message);
    return;
  }

  toast.textContent = message || "";
  toast.className = "show " + (error ? "err" : "ok");

  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => {
    toast.className = "";
  }, 3500);
}

// ================================================================
// API
// ================================================================

async function call(path, body) {
  try {
    const options = {
      method: body === undefined ? "GET" : "POST",
      headers: {
        "X-Requested-With": "hostel-ui"
      }
    };

    if (body !== undefined) {
      options.headers["Content-Type"] =
        "application/x-www-form-urlencoded;charset=UTF-8";
      options.body = new URLSearchParams(body).toString();
    }

    const response = await fetch("/api" + path, options);
    const result = await response.json();

    if (!response.ok || result.ok === false) {
      showToast(result.message || "Request failed.", true);
      return false;
    }

    if (result.state) {
      state = result.state;
      render();
    }

    if (result.message) {
      showToast(result.message);
    }

    return true;
  } catch (error) {
    console.error(error);
    showToast(
      "Cannot connect to the server. Make sure WebApp is running.",
      true
    );
    return false;
  }
}

// ================================================================
// TABLE TABS
// ================================================================

const TABS = [
  {
    key: "pending",
    label: "Pending",
    empty: 'No pending requests. Add one or click "Load sample data".',
    rows: s => s.pending || [],
    cols: [
      ["ID", r => esc(r.id)],
      ["Student", r => who(r.name, r.id)],
      ["Gender", r => gender(r.gender)],
      ["Year", r => year(r.year)],
      ["Distance", r => km(r.distanceKm)],
      ["Preferred room", r => pill(r.wants)]
    ]
  },
  {
    key: "assigned",
    label: "Assigned",
    empty: 'No students assigned yet. Click "Run allocation".',
    rows: s => s.assigned || [],
    cols: [
      ["ID", r => esc(r.id)],
      ["Student", r => who(r.name, r.id)],
      ["Gender", r => gender(r.gender)],
      ["Year", r => year(r.year)],
      ["Distance", r => km(r.distanceKm)],
      ["Room", r => `<strong>${esc(r.room || "-")}</strong>`],
      ["Type", r => pill(r.roomType)],
      ["", r => `<button type="button" class="btn mini" data-checkout="${esc(r.id)}">Check out</button>`]
    ]
  },
  {
    key: "waitlist",
    label: "Waitlist",
    empty: "The waitlist is empty.",
    rows: s => s.waitlist || [],
    cols: [
      ["Queue", r => `<span class="rank">#${esc(r.rank)}</span>`],
      ["ID", r => esc(r.id)],
      ["Student", r => who(r.name, r.id)],
      ["Gender", r => gender(r.gender)],
      ["Year", r => year(r.year)],
      ["Distance", r => km(r.distanceKm)],
      ["Waiting for", r => pill(r.wants)]
    ]
  },
  {
    key: "rejected",
    label: "Rejected",
    empty: "No rejected requests.",
    rows: s => s.rejected || [],
    cols: [
      ["ID", r => esc(r.id)],
      ["Student", r => who(r.name, r.id)],
      ["Reason", r => `<span class="reason">${esc(r.remark || "Rejected")}</span>`]
    ]
  },
  {
    key: "vacated",
    label: "Checked out",
    empty: "Nobody has checked out yet.",
    rows: s => s.vacated || [],
    cols: [
      ["ID", r => esc(r.id)],
      ["Student", r => who(r.name, r.id)],
      ["Note", r => esc(r.remark || "Checked out")]
    ]
  },
  {
    key: "log",
    label: "Event log",
    empty: "No events yet.",
    rows: s => (s.log || []).map((text, i) => ({
      n: i + 1,
      text
    })),
    cols: [
      ["#", r => esc(r.n)],
      ["Event", r => `<span class="mono">${esc(r.text)}</span>`]
    ]
  }
];

// ================================================================
// MAIN RENDERING
// ================================================================

function render() {
  if (!state) return;

  renderStats();
  renderRooms();
  renderTabs();
}

function renderStats() {
  const s = state.summary || {};

  const items = [
    ["pending", s.pending || 0, "Pending"],
    ["assigned", s.assigned || 0, "Assigned"],
    ["waitlisted", s.waitlisted || 0, "Waitlisted"],
    ["rejected", s.rejected || 0, "Rejected"],
    ["free", `${s.freeBeds || 0} / ${s.totalBeds || 0}`, "Free beds"]
  ];

  document.getElementById("stats").innerHTML = items.map(item =>
    `<div class="stat ${item[0]}">
      <div class="num">${esc(item[1])}</div>
      <div class="lbl">${item[2]}</div>
    </div>`
  ).join("");
}

// ================================================================
// ROOM DISPLAY
// ================================================================

const WINGS = [
  { gender: "MALE", title: "Boys' rooms", cls: "male" },
  { gender: "FEMALE", title: "Girls' rooms", cls: "female" }
];

function renderRooms() {
  const box = document.getElementById("rooms");
  const rooms = Array.isArray(state.rooms) ? state.rooms : [];

  if (rooms.length === 0) {
    box.innerHTML =
      '<p class="empty">No rooms yet. Add a room or load sample data.</p>';
    return;
  }

  const sections = WINGS.map(wing => {
    const wingRooms = rooms.filter(r => r.gender === wing.gender);

    if (!wingRooms.length) return "";

    const freeBeds = wingRooms.reduce((sum, room) => {
      return sum + Math.max(
        0,
        Number(room.capacity || 0) - Number(room.occupied || 0)
      );
    }, 0);

    return `
      <section class="wing ${wing.cls}">
        <h3>${wing.title}
          <span>${freeBeds} free ${freeBeds === 1 ? "bed" : "beds"}</span>
        </h3>
        <div class="room-grid">
          ${wingRooms.map(roomCard).join("")}
        </div>
      </section>
    `;
  }).join("");

  const otherRooms = rooms.filter(r =>
    !WINGS.some(wing => wing.gender === r.gender)
  );

  const otherSection = otherRooms.length
    ? `<section class="wing">
        <h3>Other rooms</h3>
        <div class="room-grid">${otherRooms.map(roomCard).join("")}</div>
      </section>`
    : "";

  box.innerHTML = sections + otherSection;
}

function roomCard(room) {
  const occupants = Array.isArray(room.occupants)
    ? room.occupants
    : [];

  const capacity = Math.max(0, Number(room.capacity) || 0);
  const occupied = occupants.length;
  const available = Math.max(0, capacity - occupied);

  let beds = "";

  for (let i = 0; i < capacity; i++) {
    const person = occupants[i];

    beds += person
      ? `<span class="bed on" title="${esc(person.name)} (${esc(person.id)})"></span>`
      : '<span class="bed" title="Available bed"></span>';
  }

  const people = occupants.length
    ? `<ul class="occupant-list">
        ${occupants.map(person => `
          <li>
            ${avatar(person.name)}
            <span class="occupant-details">
              <strong>${esc(person.name || "Unknown student")}</strong>
              <small>ID: ${esc(person.id || "Unknown")}</small>
            </span>
          </li>
        `).join("")}
      </ul>`
    : '<p class="empty room-empty">No students assigned yet</p>';

  let status;
  let statusClass;

  if (capacity > 0 && available === 0) {
    status = "Full";
    statusClass = "full";
  } else if (occupied === 0) {
    status = "Vacant";
    statusClass = "vacant";
  } else {
    status = "Partially occupied";
    statusClass = "partial";
  }

  return `
    <article class="room ${esc(String(room.type || "").toLowerCase())}">
      <div class="room-head">
        <strong>${esc(room.roomNumber || "Room")}</strong>
        ${pill(room.type)}
      </div>

      <div class="room-status ${statusClass}">${status}</div>

      <div class="beds">${beds}</div>

      <div class="room-count">
        <span>${occupied} / ${capacity} beds occupied</span>
        <span>${available} free</span>
      </div>

      <h4>Assigned students</h4>
      ${people}

      <div class="meta">
        ${esc(room.gender || "Gender not specified")}
        &middot; Rs. ${esc(room.rent == null ? "-" : room.rent)}/month
        &middot; ${esc(room.facilities || "No facilities listed")}
      </div>
    </article>
  `;
}

// ================================================================
// TABLE RENDERING
// ================================================================

function renderTabs() {
  const tabsBox = document.getElementById("tabs");
  const table = document.getElementById("table");

  tabsBox.innerHTML = TABS.map(tab => {
    const count = tab.rows(state).length;

    return `
      <button type="button"
        class="tab ${tab.key === activeTab ? "active" : ""}"
        data-tab="${tab.key}">
        ${tab.label}
        <span class="count">${count}</span>
      </button>
    `;
  }).join("");

  const tab = TABS.find(t => t.key === activeTab) || TABS[0];
  const rows = tab.rows(state);

  if (rows.length === 0) {
    table.innerHTML = `<p class="empty">${tab.empty}</p>`;
    return;
  }

  const head = "<tr>" +
    tab.cols.map(col => `<th>${col[0]}</th>`).join("") +
    "</tr>";

  const body = rows.map(row =>
    "<tr>" +
    tab.cols.map(col => `<td>${col[1](row)}</td>`).join("") +
    "</tr>"
  ).join("");

  table.innerHTML = `
    <table>
      <thead>${head}</thead>
      <tbody>${body}</tbody>
    </table>
  `;
}

// ================================================================
// FORMS AND BUTTONS
// ================================================================

function wireForm(formId, path, after) {
  const form = document.getElementById(formId);
  if (!form) return;

  form.addEventListener("submit", async event => {
    event.preventDefault();

    const values = Object.fromEntries(new FormData(form).entries());
    const ok = await call(path, values);

    if (ok) {
      form.reset();
      if (after) after();
    }
  });
}

function init() {
  wireForm("room-form", "/room");

  wireForm("request-form", "/request", () => {
    activeTab = "pending";
    renderTabs();
  });

  wireForm("checkout-form", "/checkout");

  document.getElementById("btn-sample").addEventListener("click", async () => {
    if (await call("/load-sample", {})) {
      activeTab = "pending";
      renderTabs();
    }
  });

  document.getElementById("btn-process").addEventListener("click", async () => {
    if (await call("/process", {})) {
      activeTab = "assigned";
      renderTabs();
    }
  });

  document.getElementById("btn-save").addEventListener("click", () => {
    call("/save-report", {});
  });

  document.getElementById("btn-reset").addEventListener("click", async () => {
    if (confirm("Clear all rooms, requests and results?")) {
      if (await call("/reset", {})) {
        activeTab = "pending";
      }
    }
  });

  document.getElementById("tabs").addEventListener("click", event => {
    const button = event.target.closest("[data-tab]");
    if (!button) return;

    activeTab = button.dataset.tab;
    renderTabs();
  });

  document.getElementById("table").addEventListener("click", event => {
    const button = event.target.closest("[data-checkout]");
    if (button) {
      call("/checkout", { studentId: button.dataset.checkout });
    }
  });

  call("/state");
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init);
} else {
  init();
}
