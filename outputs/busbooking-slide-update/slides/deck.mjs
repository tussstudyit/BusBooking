import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const asset = (...parts) => path.join(ROOT, "assets", ...parts);

const C = {
  navy: "#102A43",
  blue: "#1E88E5",
  sky: "#EAF4FF",
  cyan: "#1CB5E0",
  green: "#12B981",
  amber: "#F59E0B",
  red: "#EF4444",
  ink: "#172033",
  muted: "#5B687A",
  line: "#D8E2EE",
  paper: "#F8FAFF",
  white: "#FFFFFF",
  dark: "#0F172A",
};

const slideTitles = [
  "Online Intercity Bus Ticket Booking System",
  "Presentation Roadmap",
  "Manual booking leaves data fragmented across the journey.",
  "BusBooking centralizes booking, payment, and passenger control.",
  "The system now has four clear actors, including Staff.",
  "A two-app, one-web architecture keeps operations separated.",
  "Spring Boot and MySQL act as the single source of truth.",
  "The database supports booking, payment, seat, and check-in states.",
  "Use cases are grouped by real operational responsibility.",
  "The USER flow moves from search to ticket QR.",
  "The staff flow turns QR scanning into passenger check-in.",
  "The admin flow manages the whole transport operation.",
  "Payment confirmation creates valid tickets and QR evidence.",
  "User app interfaces cover the full booking journey.",
  "Staff app interfaces focus on fast station and onboard work.",
  "Web admin interfaces control trips, seats, staff, and reports.",
  "Implementation results show the system working end-to-end.",
  "Security and reliability are handled at the API and data layer.",
  "The next development step is operational scale.",
  "BusBooking delivers a complete foundation for digital bus booking.",
];

function slideBase(presentation, ctx, index, opts = {}) {
  const slide = presentation.slides.add();
  ctx.addShape(slide, { x: 0, y: 0, w: 1280, h: 720, fill: opts.dark ? C.dark : C.paper });
  if (!opts.noHeader) {
    ctx.addText(slide, {
      x: 58, y: 32, w: 170, h: 22,
      text: "BUSBOOKING",
      fontSize: 13, bold: true, color: opts.dark ? "#9BD7FF" : C.blue,
      typeface: "Aptos", align: "left", valign: "mid",
      name: `kicker-${index}-label`,
    });
    ctx.addShape(slide, { x: 58, y: 60, w: 54, h: 4, fill: C.blue });
    ctx.addText(slide, {
      x: 1130, y: 34, w: 86, h: 22,
      text: String(index).padStart(2, "0"),
      fontSize: 12, color: opts.dark ? "#B7C7D9" : C.muted,
      align: "right", valign: "mid",
    });
  }
  return slide;
}

function heading(slide, ctx, kicker, title, dark = false) {
  ctx.addText(slide, {
    x: 58, y: 86, w: 230, h: 20,
    text: kicker.toUpperCase(),
    fontSize: 12, bold: true, color: dark ? "#9BD7FF" : C.blue,
    typeface: "Aptos",
  });
  ctx.addText(slide, {
    x: 58, y: 112, w: 920, h: 96,
    text: title,
    fontSize: 36, bold: true, color: dark ? C.white : C.ink,
    typeface: "Aptos Display",
  });
}

function footer(slide, ctx, index, dark = false) {
  ctx.addShape(slide, { x: 58, y: 672, w: 1010, h: 1, fill: dark ? "#31445D" : C.line });
  ctx.addText(slide, {
    x: 58, y: 684, w: 760, h: 18,
    text: "BusBooking - Online Intercity Bus Ticket Booking System",
    fontSize: 10, color: dark ? "#9AA8B8" : C.muted,
  });
  ctx.addText(slide, {
    x: 1110, y: 684, w: 110, h: 18,
    text: `${index}/20`,
    fontSize: 10, color: dark ? "#9AA8B8" : C.muted, align: "right",
  });
}

function text(slide, ctx, x, y, w, h, value, size = 20, color = C.ink, bold = false, align = "left") {
  return ctx.addText(slide, { x, y, w, h, text: value, fontSize: size, color, bold, typeface: "Aptos", align, insets: { left: 0, right: 0, top: 0, bottom: 0 } });
}

function card(slide, ctx, x, y, w, h, title, body, color = C.blue, icon = null) {
  ctx.addShape(slide, { x, y, w, h, fill: C.white, line: ctx.line(C.line, 1) });
  ctx.addShape(slide, { x, y, w: 6, h, fill: color });
  ctx.addShape(slide, { x: x + 22, y: y + 24, w: 30, h: 30, fill: color });
  text(slide, ctx, x + 64, y + 20, w - 84, 32, title, 21, C.ink, true);
  text(slide, ctx, x + 22, y + 64, w - 44, h - 78, body, 15, C.muted);
}

function stat(slide, ctx, x, y, w, label, value, color = C.blue) {
  ctx.addShape(slide, { x, y, w, h: 94, fill: C.white, line: ctx.line(C.line, 1) });
  text(slide, ctx, x + 18, y + 16, w - 36, 34, value, 30, color, true);
  text(slide, ctx, x + 18, y + 56, w - 36, 28, label, 13, C.muted);
}

function bullet(slide, ctx, x, y, value, color = C.blue, width = 500) {
  ctx.addShape(slide, { x, y: y + 8, w: 8, h: 8, fill: color });
  text(slide, ctx, x + 22, y, width, 34, value, 18, C.ink);
}

function pill(slide, ctx, x, y, w, label, color = C.blue) {
  ctx.addShape(slide, { x, y, w, h: 38, fill: color, line: ctx.line(color, 1) });
  text(slide, ctx, x + 12, y + 8, w - 24, 18, label, 14, C.white, true, "center");
}

function arrow(slide, ctx, x1, y1, x2, y2, color = C.blue) {
  if (Math.abs(x2 - x1) >= Math.abs(y2 - y1)) {
    const x = Math.min(x1, x2), w = Math.abs(x2 - x1);
    ctx.addShape(slide, { x, y: y1 - 1, w, h: 2, fill: color });
    ctx.addShape(slide, { x: x2 - 4, y: y1 - 4, w: 8, h: 8, fill: color });
  } else {
    const y = Math.min(y1, y2), h = Math.abs(y2 - y1);
    ctx.addShape(slide, { x: x1 - 1, y, w: 2, h, fill: color });
  }
}

function phoneFrame(slide, ctx, x, y, w, h, title, rows, accent = C.blue) {
  ctx.addShape(slide, { x, y, w, h, fill: C.dark, line: ctx.line(C.dark, 1) });
  ctx.addShape(slide, { x: x + 10, y: y + 12, w: w - 20, h: h - 24, fill: C.white });
  ctx.addShape(slide, { x: x + 10, y: y + 12, w: w - 20, h: 54, fill: accent });
  text(slide, ctx, x + 22, y + 30, w - 44, 20, title, 16, C.white, true);
  let cy = y + 86;
  rows.forEach((row, i) => {
    ctx.addShape(slide, { x: x + 24, y: cy, w: w - 48, h: 42, fill: i % 2 ? "#F4F8FC" : C.sky, line: ctx.line("#EDF2F7", 1) });
    text(slide, ctx, x + 38, cy + 10, w - 76, 20, row, 12, C.ink);
    cy += 52;
  });
}

function adminFrame(slide, ctx, x, y, w, h, title, rows) {
  ctx.addShape(slide, { x, y, w, h, fill: C.white, line: ctx.line(C.line, 1) });
  ctx.addShape(slide, { x, y, w, h: 46, fill: C.navy });
  text(slide, ctx, x + 18, y + 14, w - 36, 18, title, 16, C.white, true);
  let cy = y + 66;
  rows.forEach((row, i) => {
    ctx.addShape(slide, { x: x + 18, y: cy, w: w - 36, h: 34, fill: i % 2 ? "#F8FAFC" : "#EFF6FF" });
    text(slide, ctx, x + 30, cy + 8, w - 60, 16, row, 12, C.ink);
    cy += 42;
  });
}

function miniScreen(slide, ctx, x, y, w, h, label, items, color) {
  ctx.addShape(slide, { x, y, w, h, fill: C.white, line: ctx.line(C.line, 1) });
  ctx.addShape(slide, { x, y, w, h: 38, fill: color });
  text(slide, ctx, x + 12, y + 11, w - 24, 16, label, 13, C.white, true);
  let cy = y + 54;
  for (const item of items) {
    ctx.addShape(slide, { x: x + 14, y: cy + 5, w: 7, h: 7, fill: color });
    text(slide, ctx, x + 28, cy, w - 42, 22, item, 11, C.ink);
    cy += 30;
  }
}

function flowNode(slide, ctx, x, y, w, label, sub, color, icon) {
  ctx.addShape(slide, { x, y, w, h: 104, fill: C.white, line: ctx.line(C.line, 1) });
  ctx.addShape(slide, { x, y, w, h: 8, fill: color });
  ctx.addShape(slide, { x: x + 18, y: y + 25, w: 28, h: 28, fill: color });
  text(slide, ctx, x + 58, y + 22, w - 76, 24, label, 18, C.ink, true);
  text(slide, ctx, x + 18, y + 58, w - 36, 34, sub, 13, C.muted);
}

export async function renderSlide(presentation, ctx, n) {
  const title = slideTitles[n - 1];
  const s = slideBase(presentation, ctx, n, { noHeader: n === 1, dark: n === 1 || n === 20 });
  switch (n) {
    case 1: {
      await ctx.addImage(s, { path: asset("extracted", "pptx", "pptx-003.jpeg"), x: 0, y: 0, w: 1280, h: 720, fit: "cover", alt: "Intercity buses" });
      ctx.addShape(s, { x: 0, y: 0, w: 1280, h: 720, fill: "#071525CC" });
      ctx.addText(s, { x: 72, y: 92, w: 170, h: 28, text: "BUSBOOKING", fontSize: 14, color: "#9BD7FF", bold: true });
      ctx.addText(s, { x: 70, y: 148, w: 900, h: 144, text: title, fontSize: 52, color: C.white, bold: true, typeface: "Aptos Display" });
      ctx.addText(s, { x: 72, y: 312, w: 670, h: 60, text: "Graduation project presentation - two Android apps, one Spring Boot web admin, centralized MySQL data, and VNPAY payment.", fontSize: 22, color: "#D9E8F5" });
      ctx.addShape(s, { x: 72, y: 418, w: 490, h: 150, fill: "#FFFFFFE6", line: ctx.line("#FFFFFF", 1) });
      text(s, ctx, 96, 444, 430, 22, "Students", 15, C.blue, true);
      text(s, ctx, 96, 476, 430, 22, "Đặng Tú Nguyên - 24IT181", 20, C.ink, true);
      text(s, ctx, 96, 508, 430, 22, "Trần Anh Tân - 24IT237", 20, C.ink, true);
      text(s, ctx, 96, 548, 430, 18, "Supervisor: Dr. Phạm Nguyễn Minh Nhựt", 14, C.muted);
      text(s, ctx, 72, 654, 430, 18, "Da Nang, 2026", 13, "#C7D7E8");
      return s;
    }
    case 2: {
      heading(s, ctx, "Roadmap", title);
      const agenda = [
        ["01", "Project context and objectives"],
        ["02", "Technology stack and architecture"],
        ["03", "System requirements and actors"],
        ["04", "Design: database, use cases, and workflows"],
        ["05", "Implemented interfaces and results"],
        ["06", "Evaluation, future work, and conclusion"],
      ];
      agenda.forEach((a, i) => {
        const x = 78 + (i % 2) * 560, y = 238 + Math.floor(i / 2) * 105;
        ctx.addShape(s, { x, y, w: 480, h: 78, fill: C.white, line: ctx.line(C.line, 1) });
        text(s, ctx, x + 24, y + 22, 52, 30, a[0], 28, C.blue, true);
        text(s, ctx, x + 92, y + 25, 350, 24, a[1], 19, C.ink, true);
      });
      footer(s, ctx, n);
      return s;
    }
    case 3: {
      heading(s, ctx, "Problem", title);
      card(s, ctx, 72, 242, 330, 230, "Fragmented booking", "Manual booking makes it difficult to check routes, departure times, available seats, and passenger information in real time.", C.red, "clipboard-list");
      card(s, ctx, 475, 242, 330, 230, "Payment mismatch", "Ticket status and payment status can become inconsistent when confirmation is handled outside a centralized backend.", C.amber, "credit-card");
      card(s, ctx, 878, 242, 330, 230, "Operational blind spots", "Bus staff need a fast way to verify tickets and track who has actually boarded each trip.", C.blue, "scan-qr-code");
      bullet(s, ctx, 108, 536, "The project digitalizes the full journey from published trips to passenger check-in.", C.green, 920);
      footer(s, ctx, n);
      return s;
    }
    case 4: {
      heading(s, ctx, "Scope", title);
      stat(s, ctx, 80, 238, 250, "Android modules", "2", C.blue);
      stat(s, ctx, 360, 238, 250, "Web admin + REST API", "1", C.green);
      stat(s, ctx, 640, 238, 250, "Core actors", "4", C.amber);
      stat(s, ctx, 920, 238, 250, "Payment provider", "VNPAY", C.cyan);
      const objectives = [
        "User app: search trips, choose seats, book one-way or round-trip tickets, pay online, and view ticket QR.",
        "Staff app: view assigned trips, scan QR or enter ticket code, check in passengers, and inspect seat status.",
        "Web admin: manage users, staff, routes, buses, seats, trips, tickets, payments, and revenue reports.",
      ];
      objectives.forEach((o, i) => bullet(s, ctx, 96, 386 + i * 54, o, [C.blue, C.green, C.amber][i], 970));
      footer(s, ctx, n);
      return s;
    }
    case 5: {
      heading(s, ctx, "Actors", title);
      const actors = [
        ["USER", "Search, book, pay, and present ticket QR.", C.blue, "user"],
        ["Staff", "Verify tickets, check passengers in, and monitor assigned trips.", C.green, "scan-qr-code"],
        ["Admin", "Manage master data, staff accounts, payments, and reports.", C.amber, "shield-check"],
        ["VNPAY", "External payment gateway that returns transaction results.", C.cyan, "credit-card"],
      ];
      actors.forEach((a, i) => {
        const x = 82 + i * 290;
        ctx.addShape(s, { x, y: 244, w: 246, h: 238, fill: C.white, line: ctx.line(C.line, 1) });
        ctx.addShape(s, { x, y: 244, w: 246, h: 10, fill: a[2] });
        ctx.addShape(s, { x: x + 86, y: 288, w: 74, h: 74, fill: a[2] });
        text(s, ctx, x + 86, 311, 74, 24, a[0].slice(0, 1), 30, C.white, true, "center");
        text(s, ctx, x + 24, 378, 198, 30, a[0], 24, C.ink, true, "center");
        text(s, ctx, x + 24, 414, 198, 48, a[1], 14, C.muted, false, "center");
      });
      pill(s, ctx, 262, 546, 756, "Staff is a dedicated actor. The CARRIER role is removed from the final system model.", C.navy);
      footer(s, ctx, n);
      return s;
    }
    case 6: {
      heading(s, ctx, "Technology", title);
      card(s, ctx, 72, 230, 250, 230, "User App", "Android Studio, Kotlin, XML layouts, ViewBinding, MVVM-style ViewModels, REST API client.", C.blue, "smartphone");
      card(s, ctx, 360, 230, 250, 230, "Staff App", "Separate Android module for staff login, assigned trips, QR scanning, passenger check-in, and seat map.", C.green, "qr-code");
      card(s, ctx, 648, 230, 250, 230, "Web Admin", "Java 21, Spring Boot 3.3.5, Thymeleaf, Spring Security, embedded Tomcat, VNPAY endpoints.", C.amber, "monitor");
      card(s, ctx, 936, 230, 250, 230, "Database", "MySQL through XAMPP. Tables cover users, staff, buses, trips, seats, tickets, payments, and check-ins.", C.cyan, "database");
      footer(s, ctx, n);
      return s;
    }
    case 7: {
      heading(s, ctx, "Architecture", title);
      flowNode(s, ctx, 74, 272, 230, "User App", "Search, booking, payment, ticket QR", C.blue, "smartphone");
      flowNode(s, ctx, 74, 430, 230, "Staff App", "Trip assignment, QR scan, check-in", C.green, "scan-qr-code");
      flowNode(s, ctx, 462, 346, 260, "Spring Boot Backend", "REST API, validation, security, VNPAY callback", C.amber, "server");
      flowNode(s, ctx, 880, 272, 230, "MySQL", "Centralized operational data", C.cyan, "database");
      flowNode(s, ctx, 880, 430, 230, "VNPAY", "Payment page and result callback", C.red, "credit-card");
      adminFrame(s, ctx, 462, 520, 260, 90, "Web Admin", ["Thymeleaf UI", "Runs on embedded Tomcat"]);
      arrow(s, ctx, 304, 324, 462, 398, C.blue);
      arrow(s, ctx, 304, 482, 462, 398, C.green);
      arrow(s, ctx, 722, 398, 880, 324, C.cyan);
      arrow(s, ctx, 722, 398, 880, 482, C.red);
      footer(s, ctx, n);
      return s;
    }
    case 8: {
      heading(s, ctx, "Database", title);
      const groups = [
        ["Identity", ["users", "staff_profiles", "bus_companies"], C.blue],
        ["Operations", ["routes", "buses", "seats", "trips", "trip_staff_assignments"], C.green],
        ["Booking", ["tickets", "trip_seats", "bookings", "booking_tickets"], C.amber],
        ["Payment & control", ["payments", "payment_items", "ticket_checkins"], C.cyan],
      ];
      groups.forEach((g, i) => {
        const x = 76 + i * 292;
        ctx.addShape(s, { x, y: 242, w: 250, h: 300, fill: C.white, line: ctx.line(C.line, 1) });
        ctx.addShape(s, { x, y: 242, w: 250, h: 44, fill: g[2] });
        text(s, ctx, x + 16, 255, 218, 18, g[0], 16, C.white, true, "center");
        g[1].forEach((item, j) => {
          ctx.addShape(s, { x: x + 24, y: 312 + j * 42, w: 202, h: 28, fill: "#F8FAFC", line: ctx.line("#E7EEF7", 1) });
          text(s, ctx, x + 36, 319 + j * 42, 178, 14, item, 12, C.ink, false, "center");
        });
      });
      bullet(s, ctx, 92, 590, "Ticket QR is generated from ticket status; VNPAY QR belongs to payment records.", C.blue, 960);
      footer(s, ctx, n);
      return s;
    }
    case 9: {
      heading(s, ctx, "Use cases", title);
      const cols = [
        ["USER", ["Register / log in", "Search trips", "Choose seats", "Book one-way / round-trip", "Pay with VNPAY", "View tickets and history"], C.blue],
        ["Staff", ["Log in with admin-issued email", "View assigned trips", "View passengers", "Scan QR or enter ticket code", "Confirm passenger boarding"], C.green],
        ["Admin", ["Manage USER accounts", "Create staff accounts", "Manage routes, buses, seats, trips", "Monitor tickets and payments", "View revenue statistics"], C.amber],
      ];
      cols.forEach((c, i) => {
        const x = 84 + i * 382;
        ctx.addShape(s, { x, y: 236, w: 330, h: 338, fill: C.white, line: ctx.line(C.line, 1) });
        ctx.addShape(s, { x, y: 236, w: 330, h: 48, fill: c[2] });
        text(s, ctx, x + 18, 250, 294, 18, c[0], 17, C.white, true, "center");
        c[1].forEach((item, j) => bullet(s, ctx, x + 26, 310 + j * 40, item, c[2], 276));
      });
      footer(s, ctx, n);
      return s;
    }
    case 10: {
      heading(s, ctx, "USER flow", title);
      const nodes = [
        ["Search", "Origin, destination, date", "search"],
        ["Select seat", "Seat map and price", "armchair"],
        ["Book", "Create pending ticket", "ticket"],
        ["Pay", "VNPAY URL or QR", "credit-card"],
        ["Use ticket", "Show ticket QR", "qr-code"],
      ];
      nodes.forEach((node, i) => {
        const x = 64 + i * 236;
        flowNode(s, ctx, x, 292, 190, node[0], node[1], C.blue, node[2]);
        if (i < nodes.length - 1) arrow(s, ctx, x + 190, 344, x + 236, 344, C.blue);
      });
      phoneFrame(s, ctx, 130, 496, 220, 128, "My Ticket", ["Confirmed ticket", "QR for staff scan"], C.blue);
      text(s, ctx, 420, 534, 670, 48, "The User App never connects directly to MySQL. It receives trip, seat, payment, and ticket data through Spring Boot REST APIs.", 21, C.ink, true);
      footer(s, ctx, n);
      return s;
    }
    case 11: {
      heading(s, ctx, "Staff flow", title);
      flowNode(s, ctx, 74, 286, 222, "Login", "Gmail account issued by admin", C.green, "log-in");
      flowNode(s, ctx, 336, 286, 222, "Assigned trips", "Only trips mapped to staff", C.green, "bus");
      flowNode(s, ctx, 598, 286, 222, "Verify ticket", "Scan QR or enter code", C.green, "scan-qr-code");
      flowNode(s, ctx, 860, 286, 222, "Check in", "Update ticket and seat state", C.green, "check-circle");
      arrow(s, ctx, 296, 338, 336, 338, C.green);
      arrow(s, ctx, 558, 338, 598, 338, C.green);
      arrow(s, ctx, 820, 338, 860, 338, C.green);
      ctx.addShape(s, { x: 142, y: 502, w: 998, h: 82, fill: "#ECFDF5", line: ctx.line("#A7F3D0", 1) });
      text(s, ctx, 170, 524, 940, 34, "After check-in, the seat map changes the passenger seat from booked to checked-in. The staff can tap the dark seat to review passenger and ticket details.", 19, C.ink, true, "center");
      footer(s, ctx, n);
      return s;
    }
    case 12: {
      heading(s, ctx, "Admin flow", title);
      const steps = [
        ["1", "Create master data", "Routes, buses, seats"],
        ["2", "Create trip", "Route, bus, time, price"],
        ["3", "Assign staff", "Available staff combo box"],
        ["4", "Monitor sales", "Tickets, payments, revenue"],
      ];
      steps.forEach((st, i) => {
        const x = 86 + i * 286;
        ctx.addShape(s, { x, y: 278, w: 236, h: 190, fill: C.white, line: ctx.line(C.line, 1) });
        ctx.addShape(s, { x: x + 20, y: 300, w: 44, h: 44, fill: C.amber });
        text(s, ctx, x + 20, 310, 44, 18, st[0], 18, C.white, true, "center");
        text(s, ctx, x + 82, 300, 124, 30, st[1], 18, C.ink, true);
        text(s, ctx, x + 24, 360, 188, 54, st[2], 15, C.muted, false, "center");
      });
      adminFrame(s, ctx, 310, 516, 660, 90, "Trip edit screen", ["Left: trip data form | Right: seat layout preview | Staff selector included"]);
      footer(s, ctx, n);
      return s;
    }
    case 13: {
      heading(s, ctx, "Payment and QR", title);
      const y = 276;
      flowNode(s, ctx, 76, y, 210, "Pending ticket", "Ticket is created before payment", C.amber, "ticket");
      flowNode(s, ctx, 340, y, 210, "VNPAY payment", "Backend signs URL and QR", C.red, "credit-card");
      flowNode(s, ctx, 604, y, 210, "Callback", "Checksum and amount verified", C.cyan, "shield-check");
      flowNode(s, ctx, 868, y, 210, "Confirmed ticket", "Ticket QR becomes visible", C.green, "qr-code");
      arrow(s, ctx, 286, y + 52, 340, y + 52, C.blue);
      arrow(s, ctx, 550, y + 52, 604, y + 52, C.blue);
      arrow(s, ctx, 814, y + 52, 868, y + 52, C.blue);
      ctx.addShape(s, { x: 108, y: 500, w: 1016, h: 78, fill: C.white, line: ctx.line(C.line, 1) });
      text(s, ctx, 138, 518, 970, 34, "Important distinction: VNPAY QR is used for payment; ticket QR is generated after successful payment for staff verification.", 22, C.ink, true, "center");
      footer(s, ctx, n);
      return s;
    }
    case 14: {
      heading(s, ctx, "User UI", title);
      miniScreen(s, ctx, 72, 230, 250, 300, "Home", ["Route cards", "Trip search", "Upcoming trips"], C.blue);
      miniScreen(s, ctx, 360, 230, 250, 300, "Trip List", ["Departure time", "Bus plate", "Available seats"], C.green);
      miniScreen(s, ctx, 648, 230, 250, 300, "Seat Selection", ["Available / booked", "Total fare", "Confirm booking"], C.amber);
      miniScreen(s, ctx, 936, 230, 250, 300, "Ticket Detail", ["Payment status", "Seat number", "Ticket QR"], C.cyan);
      await ctx.addImage(s, { path: asset("extracted", "pptx", "pptx-002.png"), x: 948, y: 540, w: 74, h: 130, fit: "contain", alt: "BusBooking login screenshot" });
      text(s, ctx, 1040, 578, 150, 42, "Login screen captured from the current mobile app.", 12, C.muted);
      footer(s, ctx, n);
      return s;
    }
    case 15: {
      heading(s, ctx, "Staff UI", title);
      miniScreen(s, ctx, 80, 228, 250, 318, "Staff Home", ["Staff name", "Assigned trips today", "Checked-in count"], C.green);
      miniScreen(s, ctx, 374, 228, 250, 318, "Trip Detail", ["Route and bus", "Passenger list", "Seat map"], C.blue);
      miniScreen(s, ctx, 668, 228, 250, 318, "QR Scanner", ["Open camera", "Manual ticket code", "Verify API"], C.amber);
      miniScreen(s, ctx, 962, 228, 250, 318, "Check-in Result", ["Passenger info", "Payment state", "Confirm boarding"], C.cyan);
      ctx.addShape(s, { x: 150, y: 588, w: 86, h: 38, fill: "#F3F4F6", line: ctx.line(C.line, 1) });
      ctx.addShape(s, { x: 250, y: 588, w: 86, h: 38, fill: "#DBEAFE", line: ctx.line(C.line, 1) });
      ctx.addShape(s, { x: 350, y: 588, w: 86, h: 38, fill: "#111827", line: ctx.line("#111827", 1) });
      text(s, ctx, 460, 596, 560, 18, "Seat colors: available, booked, checked-in passenger.", 16, C.ink, true);
      footer(s, ctx, n);
      return s;
    }
    case 16: {
      heading(s, ctx, "Web Admin UI", title);
      adminFrame(s, ctx, 72, 228, 350, 286, "Dashboard", ["Users", "Trips", "Tickets", "Revenue"]);
      adminFrame(s, ctx, 466, 228, 350, 286, "Trip Management", ["Date/time labels", "Bus selector", "Staff assignment", "Seat preview"]);
      adminFrame(s, ctx, 860, 228, 350, 286, "Operations", ["USER accounts", "Staff accounts", "Payments", "Reports"]);
      text(s, ctx, 156, 566, 940, 42, "The admin web is both the management interface and the backend service provider for the Android apps.", 22, C.ink, true, "center");
      footer(s, ctx, n);
      return s;
    }
    case 17: {
      heading(s, ctx, "Results", title);
      stat(s, ctx, 84, 236, 240, "Seed vehicles", "16", C.blue);
      stat(s, ctx, 364, 236, 240, "Four-city seeded trips", "504", C.green);
      stat(s, ctx, 644, 236, 240, "Core apps", "3", C.amber);
      stat(s, ctx, 924, 236, 240, "Staff check-in states", "3", C.cyan);
      const done = [
        "User App can search trips, choose seats, create tickets, pay with VNPAY, and display ticket QR.",
        "Staff app can log in, read assigned trips, scan QR, verify tickets, check in passengers, and inspect seats.",
        "Web admin can manage trips, buses, seats, users, staff assignment, tickets, payments, and statistics.",
      ];
      done.forEach((d, i) => bullet(s, ctx, 108, 388 + i * 54, d, [C.blue, C.green, C.amber][i], 960));
      footer(s, ctx, n);
      return s;
    }
    case 18: {
      heading(s, ctx, "Quality", title);
      card(s, ctx, 82, 242, 326, 244, "Security", "Role-based access separates USER, STAFF, and ADMIN. Android clients call REST APIs instead of directly accessing MySQL.", C.blue, "lock");
      card(s, ctx, 476, 242, 326, 244, "Consistency", "Seat, ticket, payment, and check-in states are stored centrally so web admin and apps share the same source of truth.", C.green, "refresh-cw");
      card(s, ctx, 870, 242, 326, 244, "Reliability", "Payment is confirmed through signed VNPAY response verification before ticket confirmation and QR availability.", C.amber, "shield-check");
      footer(s, ctx, n);
      return s;
    }
    case 19: {
      heading(s, ctx, "Future work", title);
      const roadmap = [
        ["Near term", "Polish Android UI, complete staff operational screens, and expand admin reporting."],
        ["Mid term", "Add notification, cancellation/refund workflow, route recommendations, and better seat layout tooling."],
        ["Long term", "Deploy backend to a public server, harden security, support multiple transport companies at scale."],
      ];
      roadmap.forEach((r, i) => {
        const y = 248 + i * 118;
        ctx.addShape(s, { x: 100, y, w: 1080, h: 82, fill: C.white, line: ctx.line(C.line, 1) });
        ctx.addShape(s, { x: 100, y, w: 12, h: 82, fill: [C.blue, C.green, C.amber][i] });
        text(s, ctx, 132, y + 20, 160, 28, r[0], 22, [C.blue, C.green, C.amber][i], true);
        text(s, ctx, 318, y + 22, 780, 30, r[1], 18, C.ink);
      });
      footer(s, ctx, n);
      return s;
    }
    case 20: {
      ctx.addShape(s, { x: 0, y: 0, w: 1280, h: 720, fill: C.dark });
      ctx.addShape(s, { x: 70, y: 90, w: 6, h: 448, fill: C.blue });
      ctx.addText(s, { x: 106, y: 108, w: 900, h: 110, text: title, fontSize: 46, color: C.white, bold: true, typeface: "Aptos Display" });
      const lines = [
        "Centralized Spring Boot + MySQL backend.",
        "User App for booking and payment.",
        "Staff app for QR verification and passenger boarding.",
        "Web admin for operations, assignments, and reporting.",
      ];
      lines.forEach((line, i) => {
        const color = [C.blue, C.green, C.amber, C.cyan][i];
        ctx.addShape(s, { x: 120, y: 290 + i * 52, w: 8, h: 8, fill: color });
        text(s, ctx, 142, 282 + i * 52, 620, 34, line, 20, "#EAF4FF");
      });
      ctx.addShape(s, { x: 820, y: 272, w: 300, h: 170, fill: "#FFFFFF12", line: ctx.line("#37516F", 1) });
      text(s, ctx, 850, 318, 240, 36, "Thank you", 34, C.white, true, "center");
      text(s, ctx, 850, 370, 240, 40, "Questions & discussion", 18, "#D9E8F5", false, "center");
      text(s, ctx, 106, 650, 640, 22, "Đặng Tú Nguyên - 24IT181 | Trần Anh Tân - 24IT237", 15, "#B7C7D9");
      return s;
    }
    default:
      heading(s, ctx, "Slide", title);
      footer(s, ctx, n);
      return s;
  }
}
