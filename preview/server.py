#!/usr/bin/env python3
"""In-memory preview of the asset tree.

This is not the Jira plugin. It serves the same page and REST contract.
Projects are not invented here: in Jira the list comes from the instance.
"""

import base64
import calendar
import csv
import datetime
import json
import os
import re
import threading
import zipfile
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from io import BytesIO, StringIO
from xml.etree import ElementTree as ET
from urllib.parse import parse_qs, quote, unquote, urlparse

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
PORT = 47121
FILE_DIR = "/tmp/asset-tree-preview-files"
MAX_FILE_BYTES = 10 * 1024 * 1024
MAX_COMMENT = 10000
MAX_DEPTH = 40
PALETTE = ["#0F6E56", "#175CD3", "#6554C0", "#B54708", "#B42318", "#0E7090", "#363F72", "#087443"]
STATUSES = {"in_stock", "in_use", "repair", "reserve", "maintenance", "written_off", "active", "inactive", "retired"}
CANONICAL = {"active": "in_use", "inactive": "reserve", "retired": "written_off"}
DEFAULT_STATUSES = [
    ("in_stock", "asset-tree.ui.statusStock", "todo"),
    ("in_use", "asset-tree.ui.statusInUse", "progress"),
    ("repair", "asset-tree.ui.statusRepair", "progress"),
    ("reserve", "asset-tree.ui.statusReserve", "todo"),
    ("maintenance", "asset-tree.ui.statusMaintenance", "progress"),
    ("written_off", "asset-tree.ui.statusWrittenOff", "done"),
]
SUMMARY_DEFAULTS = {"repair", "maintenance", "written_off"}
CATEGORY_COLOR = {
    "todo": "#4a6785", "progress": "#ffd351", "done": "#14892c",
    "blue": "#0052cc", "orange": "#ff8b00", "red": "#de350b", "purple": "#6554c0",
    "teal": "#00a3bf", "gray": "#6b778c", "pink": "#cd519d", "lime": "#36b37e", "brown": "#974f0c",
}
GROUPS = ["jira-administrators", "jira-servicedesk-users", "asset-keepers"]
ICONS = [
    "building", "warehouse", "department", "office", "hospital", "factory", "store", "home",
    "device", "desktop", "laptop", "monitor", "server", "printer", "scanner", "phone", "tablet",
    "camera", "network", "wifi", "storage", "keyboard", "projector", "battery",
    "medical", "microscope", "tool", "vehicle", "furniture", "box", "document", "tag",
    "project", "megaphone", "target", "bars", "headset", "users", "chat", "clipboard",
    "gear", "badge", "syringe", "pill", "pulse", "code", "window",
    "officebuilding",
    "hospitalbuilding",
    "homecity",
    "domain",
    "storefront",
    "school",
    "bank",
    "beddouble",
    "parking",
    "garage",
    "floorplan",
    "mapmarker",
    "door",
    "townhall",
    "greenhouse",
    "homegroup",
    "stadium",
    "church",
    "tent",
    "desktoptower",
    "desktopclassic",
    "monitormultiple",
    "monitordashboard",
    "servernetwork",
    "database",
    "nas",
    "harddisk",
    "usbflashdrive",
    "memory",
    "chip",
    "routerwireless",
    "routernetwork",
    "lan",
    "accesspoint",
    "ethernet",
    "cabledata",
    "printer3d",
    "fax",
    "cellphone",
    "phoneclassic",
    "phonevoip",
    "webcam",
    "cctv",
    "mouse",
    "projectorscreen",
    "television",
    "speaker",
    "microphone",
    "batterycharging",
    "powerplug",
    "cloud",
    "shieldlock",
    "key",
    "sim",
    "console",
    "application",
    "barcodescan",
    "qrcodescan",
    "cardaccountdetails",
    "pointofsale",
    "watch",
    "headphones",
    "gamepadvariant",
    "usb",
    "antenna",
    "satelliteuplink",
    "hospitalbox",
    "stethoscope",
    "heartpulse",
    "medicalbag",
    "needle",
    "bottletonicplus",
    "testtube",
    "flask",
    "thermometer",
    "wheelchair",
    "bed",
    "ambulance",
    "tooth",
    "eye",
    "lungs",
    "brain",
    "dna",
    "virus",
    "bandage",
    "mothernurse",
    "doctor",
    "facemask",
    "radioactive",
    "radiologybox",
    "medication",
    "ivbag",
    "diabetes",
    "scalebathroom",
    "bloodbag",
    "humanmaleheight",
    "babycarriage",
    "handwash",
    "clipboardpulse",
    "clipboardplus",
    "gascylinder",
    "medcross",
    "heart",
    "bone",
    "earhearing",
    "humanpregnant",
    "allergy",
    "biohazard",
    "monitoreye",
    "waterpercent",
    "scalebalance",
    "sprinkler",
    "foodapple",
    "walk",
    "humanwheelchair",
    "airconditioner",
    "fan",
    "hvac",
    "radiator",
    "thermostat",
    "waterboiler",
    "waterpump",
    "pipe",
    "pipevalve",
    "water",
    "lightbulb",
    "ceilinglight",
    "flash",
    "transmissiontower",
    "powersocketeu",
    "electricswitch",
    "fuse",
    "meterelectric",
    "gauge",
    "fireextinguisher",
    "fire",
    "smokedetector",
    "alarmlight",
    "elevator",
    "escalator",
    "stairs",
    "doorclosedlock",
    "wrench",
    "hammerwrench",
    "screwdriver",
    "toolbox",
    "ladder",
    "broom",
    "trashcan",
    "recycle",
    "forklift",
    "truck",
    "car",
    "engine",
    "solarpanel",
    "washingmachine",
    "fridge",
    "stove",
    "coffeemaker",
    "microwave",
    "shower",
    "toilet",
    "snowflake",
    "boomgate",
    "shieldhome",
    "sawblade",
    "hammer",
    "pail",
    "valve",
    "waterboileralert",
    "airfilter",
    "airpurifier",
    "windowclosedvariant",
    "blinds",
    "spraybottle",
    "vacuum",
    "transmissiontowerexport",
    "homethermometer",
    "lock",
    "keyvariant",
    "bullhorn",
    "chartbar",
    "chartline",
    "chartpie",
    "trendingup",
    "video",
    "videovintage",
    "movieopen",
    "palette",
    "brush",
    "draw",
    "image",
    "tshirtcrew",
    "gift",
    "tagmultiple",
    "sale",
    "newspaper",
    "email",
    "emailnewsletter",
    "web",
    "star",
    "thumbup",
    "sharevariant",
    "lightbulbon",
    "presentation",
    "calendarstar",
    "flag",
    "ticket",
    "bookopenvariant",
    "cardtext",
    "pencilruler",
    "formatpaint",
    "billboard",
    "partypopper",
    "trophy",
    "medal",
    "crown",
    "camerairis",
    "lightbulbgroup",
    "drone",
    "printerpos",
    "signtext",
    "bookopenpagevariant",
    "post",
    "rocketlaunch",
    "faceagent",
    "phoneintalk",
    "forum",
    "messagetext",
    "accountgroup",
    "accounttie",
    "handshake",
    "cashregister",
    "receipt",
    "creditcard",
    "ticketconfirmation",
    "calendarclock",
    "clock",
    "timersand",
    "emoticonhappy",
    "commentquote",
    "information",
    "helpcircle",
    "lifebuoy",
    "bellring",
    "desk",
    "chairrolling",
    "sofa",
    "ticketaccount",
    "monitoraccount",
    "accountvoice",
    "cardaccountphone",
    "contactlesspayment",
    "handcoin",
    "smartcardreader",
    "wateroutline",
    "coffee",
    "umbrella",
    "hanger",
    "tablefurniture",
    "filecabinet",
    "archive",
    "packagevariant",
    "briefcase",
    "folder",
    "book",
    "calculator",
    "safe",
    "clipboardtext",
    "paperclip",
    "pen",
    "desklamp",
    "calendar",
    "flower",
    "cart",
    "bookshelf",
    "wardrobe",
    "shredder",
    "scissorscutting",
    "stamper",
    "certificate",
    "seal",
    "bicycle",
    "bus",
]


def default_icon(location):
    return "building" if location else "device"


def resolve_icon(raw, location):
    icon = str(raw or "").strip().lower()
    if not icon:
        return default_icon(location)
    if icon not in ICONS:
        return None
    return icon


FIELD_KINDS = {
    "text", "textarea", "number", "user", "date",
    "select", "selects", "checks", "radio", "labels", "url", "version",
}
OPTION_KINDS = {"select", "selects", "checks", "radio"}
VERSION_VALUE = re.compile(r"^(?=.*\d)[0-9A-Za-z][0-9A-Za-z .+_-]{0,39}$")
URL_VALUE = re.compile(r"^https?://\S{1,480}$", re.IGNORECASE)


def option_lines(raw):
    return [line.strip() for line in str(raw or "").replace("\r\n", "\n").replace("\r", "\n").split("\n") if line.strip()]


def canonical_options(raw):
    lines = option_lines(raw)
    if not lines or len(lines) > 40:
        return None
    seen = set()
    kept = []
    for line in lines:
        if len(line) > 80:
            return None
        key = line.lower()
        if key in seen:
            continue
        seen.add(key)
        kept.append(line)
    return kept or None


def match_option(options, token):
    for item in options:
        if item == token:
            return item
    low = token.lower()
    for item in options:
        if item.lower() == low:
            return item
    return None


def value_tokens(raw, options):
    text = str(raw or "").replace("\r\n", "\n").replace("\r", "\n").strip()
    if not text:
        return []
    if "\n" in text:
        return option_lines(text)
    if options is not None and match_option(options, text):
        return [text]
    if "," not in text:
        return [text]
    parts = [part.strip() for part in text.split(",")]
    if any(not part for part in parts):
        return None
    return parts


def canonical_field_value(kind, options, raw):
    text = str(raw or "").strip()
    if not text:
        return ""
    if kind in ("select", "radio"):
        return match_option(options or [], text)
    if kind in ("selects", "checks"):
        tokens = value_tokens(text, options or [])
        if not tokens:
            return None
        ordered = []
        for option in options or []:
            if any(option.lower() == token.lower() for token in tokens) and option not in ordered:
                ordered.append(option)
        unique = {token.lower() for token in tokens}
        if len(ordered) != len(unique):
            return None
        return "\n".join(ordered)
    if kind == "labels":
        tokens = value_tokens(text, None)
        if not tokens or len(tokens) > 20:
            return None
        kept = []
        for token in tokens:
            if len(token) > 40 or "," in token:
                return None
            if token.lower() not in {item.lower() for item in kept}:
                kept.append(token)
        return "\n".join(kept) if kept else None
    if kind == "url":
        return text if URL_VALUE.match(text) else None
    if kind == "version":
        tokens = value_tokens(text, None)
        if not tokens or len(tokens) > 12:
            return None
        kept = []
        for token in tokens:
            if not VERSION_VALUE.match(token):
                return None
            if token.lower() not in {item.lower() for item in kept}:
                kept.append(token)
        return "\n".join(kept) if kept else None
    return text
CAP_ORDER = ["view", "places", "types", "object", "assets", "admin"]
STATUS_KEY = re.compile(r"^[a-z][a-z0-9-]{0,39}$")

PROJECTS = [{"key": "TEST", "name": "test"}, {"key": "STP", "name": "STP"}]
ISSUE_TYPES = [
    {"id": "10001", "name": "Инцидент"},
    {"id": "10002", "name": "Заявка на обслуживание"},
]
ISSUE_STATUSES = [
    {"id": "1", "name": "Открыта"},
    {"id": "3", "name": "В работе"},
    {"id": "5", "name": "Решено"},
]
USERS = {
    "ivanov": {"userKey": "ivanov", "username": "ivanov", "displayName": "Иванов Сергей", "email": "ivanov@example.com", "phone": "+7 495 000-11-22", "department": "ИТ-поддержка", "title": "Инженер", "directory": "Active Directory", "active": True},
    "petrova": {"userKey": "petrova", "username": "petrova", "displayName": "Петрова Анна", "email": "petrova@example.com", "phone": "+7 495 000-33-44", "department": "Хирургия", "title": "Старшая медсестра", "directory": "Active Directory", "active": True},
    "smirnov": {"userKey": "smirnov", "username": "smirnov", "displayName": "Смирнов Олег", "email": "smirnov@example.com", "phone": "+7 812 000-55-66", "department": "Склад", "title": "Заведующий складом", "directory": "Active Directory", "active": True},
}
ISSUES = {
    "SD-14": {"issueId": 10014, "issueKey": "SD-14", "projectKey": "IT", "summary": "Не открывается почта на 3 этаже", "status": "В работе"},
    "SD-22": {"issueId": 10022, "issueKey": "SD-22", "projectKey": "IT", "summary": "Замена коммутатора в серверной", "status": "Открыта"},
    "MED-7": {"issueId": 20007, "issueKey": "MED-7", "projectKey": "MED", "summary": "Списать монитор после ремонта", "status": "Ожидание"},
    "STP-1": {"issueId": 30001, "issueKey": "STP-1", "projectKey": "TEST", "summary": "Aser", "status": "Ожидание поддержки"},
    "STP-7": {"issueId": 30007, "issueKey": "STP-7", "projectKey": "STP", "summary": "Замена ноутбука", "status": "В работе"},
}
ISSUES_BY_ID = {item["issueId"]: item for item in ISSUES.values()}
SEEDS = [
    ("warehouse", "#175CD3", "warehouse", 0, True, [("address", "asset-tree.field.address", "text", True), ("phone", "asset-tree.field.phone", "text", False)]),
    ("branch", "#0E7090", "building", 1, True, [("address", "asset-tree.field.address", "text", True), ("phone", "asset-tree.field.phone", "text", False)]),
    ("department", "#6554C0", "department", 2, True, [("phone", "asset-tree.field.phone", "text", False)]),
    ("equipment", "#0F6E56", "device", 3, False, [("inventory", "asset-tree.field.inventory", "text", False), ("serial", "asset-tree.field.serial", "text", False)]),
]

LOCK = threading.Lock()
STATE = {
    "seq": 1, "assets": {}, "types": {}, "links": [], "checks": {},
    "portal_rules": [], "portal_seq": 1, "portal_demo": False,
    "flows": {}, "flow_seq": 1, "flow_graphs": {},
    "comment_seq": 1, "file_seq": 1, "comments": {}, "files": {},
    "activity_seq": 1, "activities": {}, "statuses": {}, "grants": {},
    "plan_seq": 1, "plans": {}, "mails": [],
    "duty_seq": 1, "duties": {},
    "floor_seq": 1, "floors": {},
    "report_seq": 1, "saved_reports": {},
    "lives": {},
    "cards": {}, "card_images": {},
    "assetKeys": {}, "ready": False, "views": {},
}


CARD_BLOCKS = {"name", "key", "type", "place", "status", "custodian", "project", "description", "scan"}
CARD_SIZES = {"card", "wide", "label", "custom"}
CARD_QRS = {"left", "right", "top", "none"}
CARD_KINDS = {"text", "field", "qr", "logo", "picture"}
CARD_FONTS = {"sans", "serif", "mono"}
CARD_ALIGNS = {"left", "center", "right"}
CARD_ATTR = re.compile(r"^attr:[a-z][a-z0-9-]{0,39}$")
CARD_ID = re.compile(r"^[a-z0-9]{4,16}$")
CARD_COLOR = re.compile(r"^#[0-9a-fA-F]{6}$")
CARD_SRC = re.compile(r"^data:image/(png|jpeg);base64,[A-Za-z0-9+/]+=*$")


def default_card():
    return {
        "size": "card", "qr": "left", "lines": ["name", "type", "place", "key", "scan"],
        "width": 86.0, "height": 54.0, "grid": 2,
    }


def card_field_token(raw):
    trimmed = str(raw or "").strip()
    if not trimmed:
        return ""
    lower = trimmed.lower()
    token = lower if lower in CARD_BLOCKS else trimmed
    if token in CARD_BLOCKS or CARD_ATTR.match(token):
        return token
    return ""


def card_edge(value, limit, fallback):
    try:
        number = float(value)
    except (TypeError, ValueError):
        return float(fallback)
    if number < 15:
        return 15.0
    if number > limit:
        return float(limit)
    return round(number, 2)


def apply_sheet(layout, size, width, height):
    if size == "custom":
        layout["size"] = "custom"
        layout["width"] = card_edge(width, 210, 86)
        layout["height"] = card_edge(height, 297, 54)
        return
    if size == "wide":
        layout["size"] = "wide"
        layout["width"], layout["height"] = 100.0, 64.0
        return
    if size == "label":
        layout["size"] = "label"
        layout["width"], layout["height"] = 58.0, 36.0
        return
    layout["size"] = "card"
    layout["width"], layout["height"] = 86.0, 54.0


def apply_grid(raw):
    try:
        grid = int(raw)
    except (TypeError, ValueError):
        return 2
    if grid in (0, 1, 2, 5):
        return grid
    return 2


def card_text(raw):
    out = []
    space = False
    breaks = 0
    for char in str(raw or ""):
        if len(out) >= 180:
            break
        if char == "\r":
            continue
        if char == "\n":
            if not out or out[-1] == "\n" or breaks >= 8:
                continue
            out.append("\n")
            breaks += 1
            space = False
            continue
        if char in (" ", "\t"):
            space = bool(out) and out[-1] != "\n"
            continue
        if ord(char) < 32:
            continue
        if space:
            out.append(" ")
            space = False
        out.append(char)
    return "".join(out).strip()


def normalize_elements(raw, width, height):
    elements = []
    ids = set()
    fields = set()
    asset_code = False
    picture = False
    serial = 0
    for item in raw or []:
        if not isinstance(item, dict) or len(elements) >= 24:
            continue
        kind = str(item.get("kind") or "").strip().lower()
        if kind not in CARD_KINDS:
            continue
        kept = {
            "kind": kind, "token": "", "text": "", "src": "",
            "bold": bool(item.get("bold")),
        }
        if kind == "field":
            token = card_field_token(item.get("token"))
            if not token or token in fields:
                continue
            fields.add(token)
            kept["token"] = token
        elif kind == "qr":
            mode = str(item.get("token") or "").strip().lower()
            if mode and mode != "asset":
                continue
            if asset_code:
                continue
            asset_code = True
            kept["token"] = "asset"
        elif kind == "logo":
            src = str(item.get("src") or "").strip()
            if len(src) > 90000 or not CARD_SRC.match(src):
                continue
            kept["src"] = src
        elif kind == "picture":
            if picture:
                continue
            picture = True
        else:
            caption = card_text(item.get("text"))
            if not caption:
                continue
            kept["text"] = caption
        element_id = str(item.get("id") or "").strip().lower()
        if not CARD_ID.match(element_id) or element_id in ids:
            probe = serial + 100000
            element_id = format(probe, "x")
            while not CARD_ID.match(element_id) or element_id in ids:
                probe += 1
                element_id = format(probe, "x")
        ids.add(element_id)
        serial += 1
        kept["id"] = element_id
        box = 22.0 if kind in ("qr", "logo", "picture") else 36.0
        tall = 22.0 if kind in ("qr", "logo", "picture") else 8.0
        try:
            given_w = float(item.get("w") or 0)
            given_h = float(item.get("h") or 0)
            given_x = float(item.get("x") or 0)
            given_y = float(item.get("y") or 0)
        except (TypeError, ValueError):
            given_w = given_h = given_x = given_y = 0
        if given_w >= 4:
            box = given_w
        if given_h >= 4:
            tall = given_h
        box = max(4.0, min(width, round(box, 2)))
        tall = max(4.0, min(height, round(tall, 2)))
        kept["w"] = box
        kept["h"] = tall
        kept["x"] = max(0.0, min(width - 4, round(given_x, 2)))
        kept["y"] = max(0.0, min(height - 4, round(given_y, 2)))
        try:
            font_size = int(item.get("size") or 0)
        except (TypeError, ValueError):
            font_size = 0
        kept["size"] = font_size if 6 <= font_size <= 36 else 11
        color = str(item.get("color") or "").strip()
        kept["color"] = color.lower() if CARD_COLOR.match(color) else "#172b4d"
        font = str(item.get("font") or "").strip().lower()
        kept["font"] = font if font in CARD_FONTS else "sans"
        align = str(item.get("align") or "").strip().lower()
        kept["align"] = align if align in CARD_ALIGNS else "left"
        elements.append(kept)
    return elements


def normalize_card(raw):
    layout = default_card()
    if not isinstance(raw, dict):
        return layout
    size = str(raw.get("size") or "").strip().lower()
    qr = str(raw.get("qr") or "").strip().lower()
    if size not in CARD_SIZES:
        size = "card"
    apply_sheet(layout, size, raw.get("width"), raw.get("height"))
    layout["grid"] = apply_grid(raw.get("grid"))
    if qr in CARD_QRS:
        layout["qr"] = qr
    if raw.get("lines") is not None:
        lines = []
        seen = set()
        for line in raw.get("lines") or []:
            token = card_field_token(line)
            if not token or token in seen:
                continue
            seen.add(token)
            lines.append(token)
            if len(lines) >= 12:
                break
        layout["lines"] = lines
    if raw.get("elements") is not None:
        layout["elements"] = normalize_elements(raw.get("elements"), layout["width"], layout["height"])
        layout["lines"] = [item["token"] for item in layout["elements"] if item["kind"] == "field"]
        layout["qr"] = "left" if any(item["kind"] == "qr" and item["token"] == "asset" for item in layout["elements"]) else "none"
    return layout


def card_for(key):
    stored = STATE.get("cards", {}).get(key)
    if not stored:
        return default_card()
    return normalize_card(stored)


def project_rights():
    return {
        "canEdit": True,
        "canChange": True,
        "canCreate": True,
        "canMove": True,
        "canRemove": True,
        "canComment": True,
        "canConfigure": True,
        "canGrant": True,
        "canPlaces": True,
        "canObjects": True,
        "canTypes": True,
        "canAssets": True,
        "canAdmin": True,
        "canArrange": True,
    }


def caps_from_level(level):
    if level in ("manage", "assets"):
        return "view,places,types,object,assets"
    if level == "admin":
        return ",".join(CAP_ORDER)
    if level == "edit":
        return "view,places,object"
    if level == "view":
        return "view"
    return ""


def normalize_caps(raw):
    chosen = set()
    for token in str(raw or "").split(","):
        token = token.strip()
        if token in ("create", "edit", "move", "remove", "comment"):
            chosen.update(("places", "object"))
        elif token == "schema":
            chosen.add("types")
        elif token == "access":
            chosen.add("assets")
        elif token in CAP_ORDER:
            chosen.add(token)
    if not chosen:
        return ""
    if "admin" in chosen:
        chosen.add("assets")
    if "assets" in chosen:
        chosen.update(("places", "types", "object"))
    if chosen - {"view"}:
        chosen.add("view")
    return ",".join(key for key in CAP_ORDER if key in chosen)


def level_of(caps):
    parts = {item for item in str(caps or "").split(",") if item}
    if "admin" in parts:
        return "admin"
    if "assets" in parts:
        return "assets"
    if parts & {"places", "types", "object"}:
        return "edit"
    return "view"


def unescape_properties(value):
    output = []
    index = 0
    while index < len(value):
        if value[index] == "\\" and index + 1 < len(value):
            code = value[index + 1]
            if code == "u" and index + 5 < len(value):
                output.append(chr(int(value[index + 2:index + 6], 16)))
                index += 6
                continue
            output.append({"n": "\n", "t": "\t", "r": "\r", "\\": "\\"}.get(code, code))
            index += 2
            continue
        output.append(value[index])
        index += 1
    return "".join(output)


def load_properties(path):
    result = {}
    if not os.path.exists(path):
        return result
    with open(path, "r", encoding="latin-1") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#") or line.startswith("!") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            result[key.strip()] = unescape_properties(value.strip())
    return result


def messages(lang):
    english = load_properties(os.path.join(RES, "i18n", "asset-tree.properties"))
    if lang.startswith("ru"):
        english.update(load_properties(os.path.join(RES, "i18n", "asset-tree_ru_RU.properties")))
    return english


def label_of(stored, text):
    if stored and stored.startswith("asset-tree."):
        return text.get(stored, stored)
    return stored


def canonical(status):
    return CANONICAL.get(status or "", status or "in_use")


def slug(label):
    builder = []
    dash = False
    for char in (label or "").strip().lower():
        if "a" <= char <= "z" or "0" <= char <= "9":
            builder.append(char)
            dash = False
        elif char in " -_." and builder and not dash:
            builder.append("-")
            dash = True
    text = "".join(builder).strip("-")[:40].strip("-")
    return text


def unique_key(base, taken):
    seed = base or ""
    if not seed or not ("a" <= seed[0] <= "z"):
        seed = "type" if not seed else "type-" + seed
    seed = seed[:40].strip("-") or "type"
    if seed not in taken:
        return seed
    index = 2
    while index < 10000:
        suffix = "-%s" % index
        head = seed
        if len(head) + len(suffix) > 40:
            head = head[:40 - len(suffix)].strip("-") or "type"
        candidate = head + suffix
        if candidate not in taken:
            return candidate
        index += 1
    return seed


def shows_in_summary(row):
    mode = int(row.get("summaryMode") or 0)
    if mode == 1:
        return True
    if mode == 2:
        return False
    return row.get("statusKey") in SUMMARY_DEFAULTS


def status_payload(row, count):
    return {
        "statusKey": row["statusKey"],
        "label": row["label"],
        "category": row["category"],
        "sortOrder": row["sortOrder"],
        "assetCount": count,
        "inSummary": shows_in_summary(row),
    }


def ensure_statuses(project_key, text):
    bucket = STATE["statuses"].setdefault(project_key, [])
    if bucket:
        return bucket
    for index, item in enumerate(DEFAULT_STATUSES):
        bucket.append({
            "statusKey": item[0],
            "label": text.get(item[1], item[0]),
            "category": item[2],
            "sortOrder": index,
            "summaryMode": 1 if item[0] in SUMMARY_DEFAULTS else 0,
        })
    return bucket


def status_dtos(project_key, text):
    rows = ensure_statuses(project_key, text)
    assets = [asset for asset in project_assets(project_key) if not is_location_id(asset["id"])]
    result = []
    for row in rows:
        count = len([asset for asset in assets if canonical(asset.get("status")) == row["statusKey"]])
        result.append(status_payload(row, count))
    return result


def status_known(project_key, status, text):
    key = canonical(status)
    return any(row["statusKey"] == key for row in ensure_statuses(project_key, text))


def ensure_portal_demo():
    if STATE.get("portal_demo"):
        return
    STATE["portal_demo"] = True

    def ensure_type(key, label, location, icon, order):
        if key not in STATE["types"]:
            STATE["types"][key] = {
                "typeKey": key, "projectKey": "TEST", "baseKey": key, "label": label,
                "color": "#0052CC", "icon": icon, "systemType": False, "location": location,
                "showInTree": location, "service": False, "sortOrder": order, "fields": [],
            }

    ensure_type("test-place", "Площадка", True, "building", 10)
    ensure_type("test-dept", "Отделение", True, "department", 11)
    ensure_type("test-device", "Оборудование", False, "device", 12)
    place_a = add_asset("TEST", "test-place", "Площадка А", None, "in_use", None, {}, "TEST-1")
    place_b = add_asset("TEST", "test-place", "Площадка Б", None, "in_use", None, {}, "TEST-2")
    department = add_asset("TEST", "test-dept", "Отделение А", place_a, "in_use", None, {}, "TEST-3")
    add_asset("TEST", "test-device", "Аппарат", department, "in_use", None, {}, "TEST-4")
    add_asset("TEST", "test-device", "Управляющая компания", department, "in_use", None, {}, "TEST-5")
    add_asset("TEST", "test-device", "Принтер Б", place_b, "in_use", None, {}, "TEST-6")
    STATE["portal_rules"] = [
        {"id": 1, "assetId": place_a, "position": 1, "conditions": [{"field": "Площадка", "option": "Пункт А"}]},
        {"id": 2, "assetId": place_b, "position": 2, "conditions": [{"field": "Площадка", "option": "Пункт Б"}]},
        {"id": 3, "assetId": department, "position": 3, "conditions": [
            {"field": "Площадка", "option": "Пункт А"},
            {"field": "Отделение", "option": "Пункт А"},
        ]},
    ]
    STATE["portal_seq"] = 4


def portal_rule_dto(rule):
    asset = STATE["assets"].get(rule["assetId"])
    names = []
    cursor = asset
    guard = 0
    while cursor and guard < 40:
        names.insert(0, cursor["name"])
        cursor = STATE["assets"].get(cursor.get("parentId")) if cursor.get("parentId") else None
        guard += 1
    return {
        "id": rule["id"],
        "assetId": rule["assetId"],
        "assetName": asset["name"] if asset else "",
        "assetPath": " / ".join(names),
        "position": rule["position"],
        "conditions": rule["conditions"],
    }


def seed_types(project_key):
    for base, color, icon, order, location, fields in SEEDS:
        type_key = "%s-%s" % (project_key.lower(), base)
        if type_key not in STATE["types"]:
            STATE["types"][type_key] = {
                "typeKey": type_key, "projectKey": project_key, "baseKey": base, "label": base,
                "color": color, "icon": icon, "systemType": True, "location": location, "showInTree": location,
                "service": False,
                "sortOrder": order,
                "fields": [{"fieldKey": item[0], "label": item[1], "kind": item[2], "required": item[3], "position": index} for index, item in enumerate(fields)],
            }


def copy_name(base, word, taken):
    stem = (base or "").strip()
    label = (word or "").strip() or "copy"
    for number in range(1, 1000):
        suffix = " (%s)" % label if number == 1 else " (%s %s)" % (label, number)
        room = 255 - len(suffix)
        if room < 1:
            room = 1
        head = stem[:room].rstrip() if len(stem) > room else stem
        candidate = head + suffix
        if len(candidate) > 255:
            candidate = candidate[:255]
        if candidate not in taken:
            return candidate
    return stem[:255]


ASSET_KEY = re.compile(r"^[A-Z][A-Z0-9]{1,9}$")


def asset_prefix(project):
    return (STATE.get("assetKeys") or {}).get(project) or ""


def next_object_key(project):
    prefix = asset_prefix(project)
    if not prefix:
        return None
    head = prefix + "-"
    highest = 0
    for asset in STATE["assets"].values():
        key = str(asset.get("objectKey") or "")
        if len(key) > len(head) and key.upper().startswith(head) and key[len(head):].isdigit():
            highest = max(highest, int(key[len(head):]))
    return "%s-%s" % (prefix, highest + 1)


VIEW_BASE = ("key", "type", "status", "custodian", "location", "description", "created", "updated")
KNOWN_MODULES = ("service", "map", "reports", "life", "portal", "card")


def stored_modules(project_key):
    view = (STATE.get("views") or {}).get(project_key) or {}
    present = {str(item or "").strip().lower() for item in (view.get("modules") or [])}
    return [key for key in KNOWN_MODULES if key in present]


def module_on(project_key, key):
    return key in stored_modules(project_key)


def reject_module(project_key, module, text):
    if project_key not in {item["key"] for item in PROJECTS}:
        return 400, {"message": text["asset-tree.error.project.required"]}
    if not module_on(project_key, module):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    return None


def module_setup(stored):
    stored = stored or {}
    kinds = [key for key in ("to", "kts", "verify") if key in str(stored.get("scheduleKinds") or "to,kts,verify").split(",")]
    unit = stored.get("scheduleUnit") or "month"
    if unit not in ("once", "day", "week", "month", "year"):
        unit = "month"
    hidden = [key for key in str(stored.get("reportHidden") or "").split(",") if key in REPORT_STANDARD]
    grid = int(stored.get("mapGrid") or 24)
    if grid not in (8, 12, 16, 24, 32):
        grid = 24
    currency = str(stored.get("lifeCurrency") or "RUB").strip().upper()
    if len(currency) != 3 or not currency.isalpha():
        currency = "RUB"
    approver = str(stored.get("lifeApprover") or "")
    if not re.fullmatch(r"[A-Za-z0-9._@-]{0,80}", approver):
        approver = ""
    return {
        "scheduleKinds": ",".join(kinds or ["to", "kts", "verify"]),
        "scheduleUnit": unit,
        "scheduleNotify": bool(stored.get("scheduleNotify")),
        "mapWidth": plan_limit(stored.get("mapWidth"), 1200, 640, 2400),
        "mapHeight": plan_limit(stored.get("mapHeight"), 760, 480, 1600),
        "mapGrid": grid,
        "mapSnap": stored.get("mapSnap") is not False,
        "reportHidden": ",".join(hidden),
        "lifeApproval": stored.get("lifeApproval") is not False,
        "lifeApprover": approver,
        "lifeCurrency": currency,
        "lifeMonths": max(0, min(600, int(stored.get("lifeMonths") or 0))),
    }


def plan_limit(raw, default, low, high):
    try:
        value = int(raw or 0)
    except (TypeError, ValueError):
        value = 0
    if value <= 0:
        value = default
    return max(low, min(high, value))


def room_color(raw):
    text = str(raw or "").strip().lower()
    return text if re.fullmatch(r"#[0-9a-f]{6}", text) else ""


def project_view(project_key):
    view = (STATE.get("views") or {}).get(project_key) or {}
    return {
        "treeObjects": bool(view.get("treeObjects")),
        "columns": list(view.get("columns") or []),
        "modules": stored_modules(project_key),
        "setup": module_setup(view.get("setup")),
    }


def save_project_view(project_key, body, text):
    body = body or {}
    view = STATE.setdefault("views", {}).setdefault(project_key, {})
    if body.get("treeObjects") is not None:
        view["treeObjects"] = bool(body.get("treeObjects"))
    if body.get("columns") is not None:
        picked = []
        for raw in body.get("columns") or []:
            key = (raw or "").strip()
            if not key or key == "name" or key in picked:
                continue
            if key not in VIEW_BASE and not re.fullmatch(r"attr:[a-z][a-z0-9-]{0,39}(:[a-z][a-z0-9-]{0,39})?", key):
                return 400, {"message": text.get("asset-tree.error.view.columns", "Columns")}
            picked.append(key)
        view["columns"] = picked
    if body.get("modules") is not None:
        present = set()
        for raw in body.get("modules") or []:
            key = str(raw or "").strip().lower()
            if key in KNOWN_MODULES:
                present.add(key)
        view["modules"] = [key for key in KNOWN_MODULES if key in present]
    if isinstance(body.get("setup"), dict):
        view["setup"] = module_setup(body.get("setup"))
    return 200, project_view(project_key)


def save_asset_key(project_key, body, text):
    if project_key not in {item["key"] for item in PROJECTS}:
        return 400, {"message": text["asset-tree.error.project.required"]}
    prefix = ((body or {}).get("assetKey") or "").strip().upper()
    if not prefix:
        return 400, {"message": text.get("asset-tree.error.assetKey.required", "Key")}
    if not ASSET_KEY.match(prefix):
        return 400, {"message": text.get("asset-tree.error.assetKey.format", "Format")}
    if prefix in {item["key"].upper() for item in PROJECTS}:
        return 400, {"message": text.get("asset-tree.error.assetKey.project", "Project")}
    for key, stored in (STATE.get("assetKeys") or {}).items():
        if key != project_key and str(stored).upper() == prefix:
            return 400, {"message": text.get("asset-tree.error.assetKey.taken", "Taken")}
    head = prefix + "-"
    for asset in STATE["assets"].values():
        object_key = str(asset.get("objectKey") or "")
        if asset.get("projectKey") == project_key:
            continue
        if len(object_key) > len(head) and object_key.upper().startswith(head) and object_key[len(head):].isdigit():
            return 400, {"message": text.get("asset-tree.error.assetKey.taken", "Taken")}
    STATE.setdefault("assetKeys", {})[project_key] = prefix
    return 200, {"assetKey": prefix}


def add_asset(project, type_key, name, parent, status, custodian, values, object_key=None):
    key = object_key or next_object_key(project)
    if not key:
        return None
    asset_id = STATE["seq"]
    STATE["seq"] += 1
    STATE["assets"][asset_id] = {
        "id": asset_id, "objectKey": key, "projectKey": project, "name": name,
        "description": "", "typeKey": type_key, "status": status, "parentId": parent,
        "sortOrder": asset_id, "custodianKey": custodian, "created": "2026-09-26T09:00:00Z",
        "updated": "2026-09-26T09:00:00Z", "values": values, "offeredTypes": [],
    }
    return asset_id


def seed():
    if STATE.get("ready"):
        return
    STATE["ready"] = True
    STATE["types"]["stp-place"] = {
        "typeKey": "stp-place", "projectKey": "STP", "baseKey": "place", "label": "Площадка",
        "color": "#0052CC", "icon": "building", "systemType": False, "location": True,
        "showInTree": True, "service": False, "sortOrder": 1, "fields": [],
    }
    add_asset("STP", "stp-place", "Усть-Лабинск", None, "in_use", None, {}, "STP-1")


def normalize_parent(parent_id):
    if parent_id in (None, "", 0):
        return None
    return int(parent_id)


def project_assets(project_key):
    return [asset for asset in STATE["assets"].values() if asset["projectKey"] == project_key]


def parent_map(rows):
    return {asset["id"]: normalize_parent(asset["parentId"]) for asset in rows}


def children_map(rows):
    children = {}
    for asset in rows:
        parent = normalize_parent(asset["parentId"])
        if parent is not None:
            children.setdefault(parent, []).append(asset["id"])
    return children


def would_cycle(rows, moving_id, new_parent):
    parents = parent_map(rows)
    cursor = new_parent
    guard = 0
    while cursor is not None and guard < 80:
        if cursor == moving_id:
            return True
        cursor = parents.get(cursor)
        guard += 1
    return False


def descendants(rows, asset_id):
    children = children_map(rows)
    ordered = []

    def walk(current):
        for child in children.get(current, []):
            ordered.append(child)
            walk(child)

    walk(asset_id)
    return ordered


def depth(rows, asset_id):
    parents = parent_map(rows)
    seen = 0
    cursor = parents.get(asset_id)
    while cursor is not None and seen < 80:
        seen += 1
        cursor = parents.get(cursor)
    return seen


def subtree_height(rows, asset_id):
    children = children_map(rows)

    def height(current):
        kids = children.get(current, [])
        return 0 if not kids else 1 + max(height(child) for child in kids)

    return height(asset_id)


def location_of(asset):
    names = []
    parent = normalize_parent(asset["parentId"])
    guard = 0
    while parent and guard < 80:
        node = STATE["assets"].get(parent)
        if not node:
            break
        names.insert(0, node["name"])
        parent = normalize_parent(node["parentId"])
        guard += 1
    return " / ".join(names)


def type_row(type_key):
    return STATE["types"].get(type_key)


def canonical_date(value):
    text = (value or "").strip()
    if not text:
        return ""
    patterns = (
        (r"^(\d{4})-(\d{2})-(\d{2})$", "iso"),
        (r"^(\d{1,2})\.(\d{1,2})\.(\d{4})$", "dmy"),
        (r"^(\d{1,2})\.(\d{1,2})\.(\d{2})$", "short"),
    )
    for pattern, shape in patterns:
        match = re.fullmatch(pattern, text)
        if not match:
            continue
        if shape == "iso":
            year, month, day = int(match.group(1)), int(match.group(2)), int(match.group(3))
        elif shape == "dmy":
            year, month, day = int(match.group(3)), int(match.group(2)), int(match.group(1))
        else:
            yy = int(match.group(3))
            year = 2000 + yy if yy <= 69 else 1900 + yy
            month, day = int(match.group(2)), int(match.group(1))
        if year < 1900 or year > 2199:
            return None
        try:
            return datetime.date(year, month, day).isoformat()
        except ValueError:
            return None
    return None


def type_dto(row, text, rows):
    label = row["label"]
    if row["systemType"] and row.get("baseKey"):
        label = text.get("asset-tree.type." + row["baseKey"], label)
    count = len([asset for asset in rows if asset["typeKey"] == row["typeKey"]])
    return {
        "typeKey": row["typeKey"], "projectKey": row["projectKey"], "label": label, "color": row["color"],
        "icon": row.get("icon") if row.get("icon") in ICONS else default_icon(row["location"]),
        "systemType": row["systemType"], "location": row["location"],
        "placeCaption": (row.get("placeCaption") or "") if row.get("location") else "",
        "showInTree": bool(row.get("location") or row.get("showInTree")),
        "parentTypeKey": (row.get("parentTypeKey") or ""),
        "service": bool(row.get("service")) and not row.get("location"), "assetCount": count,
        "fields": [{
            "fieldKey": field["fieldKey"], "label": label_of(field["label"], text), "kind": field["kind"],
            "required": field["required"], "position": field["position"],
            "showOnIssue": field["showOnIssue"] if "showOnIssue" in field else True,
            "options": field.get("options") or [],
        } for field in row["fields"]],
    }


def parent_key(row):
    return ((row or {}).get("parentTypeKey") or "").strip()


def picker_nodes(rows, text):
    ordered = sorted(rows, key=lambda item: (item["sortOrder"], item["id"]))
    sources = []
    for row in ordered:
        typed = type_row(row["typeKey"])
        if typed:
            dto = type_dto(typed, text, ordered)
            location = bool(dto.get("location"))
            type_label = dto.get("label") or row["typeKey"]
            parent_type_key = (dto.get("parentTypeKey") or "").strip()
        else:
            location = False
            type_label = row["typeKey"]
            parent_type_key = ""
        parent_label = parent_type_key
        if parent_type_key:
            parent_type = type_row(parent_type_key)
            if parent_type:
                parent_label = type_dto(parent_type, text, ordered).get("label") or parent_type_key
        sources.append({
            "id": row["id"],
            "parentId": normalize_parent(row["parentId"]),
            "name": row["name"] or "",
            "objectKey": row["objectKey"] or "",
            "status": canonical(row["status"]),
            "typeKey": row["typeKey"] or "",
            "typeLabel": type_label or row["typeKey"] or "",
            "parentTypeKey": parent_type_key,
            "parentTypeLabel": parent_label,
            "location": location,
            "offeredLines": list(row.get("offeredTypes") or []) if location else [],
        })
    return arrange_picker(sources)


def arrange_picker(sources):
    nodes = []
    equipment_by_place = {}
    for source in sources:
        if source["location"] or source["parentId"] is None:
            continue
        equipment_by_place.setdefault(source["parentId"], []).append(source)
    next_group = [-1]
    placed = set()
    for source in sources:
        if not source["location"]:
            continue
        nodes.append(picker_asset(source, source["parentId"]))
        gear = equipment_by_place.get(source["id"])
        if not gear:
            continue
        append_picker_groups(nodes, source["id"], gear, next_group, placed, source.get("offeredLines") or [])
    for source in sources:
        if source["location"] or source["id"] in placed:
            continue
        nodes.append(picker_asset(source, source["parentId"]))
    return nodes


def append_picker_groups(nodes, place_id, gear, next_group, placed, lines=None):
    facts = []
    labels = {}
    seen = set()
    project = ""
    if gear:
        typed = type_row(gear[0]["typeKey"])
        if typed:
            project = typed.get("projectKey") or ""
    for row in place_facts(project) if project else []:
        key = row.get("typeKey") or ""
        if not key or key in seen:
            continue
        facts.append(row)
        labels[key] = row.get("label") or key
        seen.add(key)
    pinned = []
    by_type = {}
    for item in gear:
        key = item["typeKey"]
        if key not in pinned:
            pinned.append(key)
        by_type.setdefault(key, []).append(item)
        if key not in seen:
            facts.append({"typeKey": key, "parentTypeKey": item.get("parentTypeKey") or "", "label": item["typeLabel"]})
            labels[key] = item["typeLabel"]
            seen.add(key)
    slots = place_layout(lines or [], pinned, facts)
    roots = []
    children = {}
    for slot in slots:
        if not slot["parentKey"]:
            roots.append(slot["typeKey"])
        else:
            children.setdefault(slot["parentKey"], []).append(slot["typeKey"])

    def used(key):
        if by_type.get(key):
            return True
        return any(by_type.get(child) for child in children.get(key, []))

    for root in roots:
        if not used(root):
            continue
        root_id = next_group[0]
        next_group[0] = root_id - 1
        nodes.append(picker_group(root_id, place_id, labels.get(root) or root))
        for item in by_type.get(root, []):
            nodes.append(picker_asset(item, root_id))
            placed.add(item["id"])
        for child in children.get(root, []):
            if not by_type.get(child):
                continue
            child_id = next_group[0]
            next_group[0] = child_id - 1
            nodes.append(picker_group(child_id, root_id, labels.get(child) or child))
            for item in by_type[child]:
                nodes.append(picker_asset(item, child_id))
                placed.add(item["id"])


def picker_root_label(root_key, items):
    for item in items:
        if item["parentTypeKey"]:
            return item["parentTypeLabel"] or root_key
    return items[0]["typeLabel"] or root_key


def picker_asset(source, parent_id):
    return {
        "id": source["id"],
        "parentId": parent_id,
        "name": source["name"],
        "objectKey": source["objectKey"],
        "status": source["status"],
        "typeLabel": source["typeLabel"],
        "group": False,
    }


def picker_group(group_id, parent_id, label):
    return {
        "id": group_id,
        "parentId": parent_id,
        "name": label,
        "objectKey": "",
        "status": "",
        "typeLabel": label,
        "group": True,
    }


def child_types(type_key):
    return [row for row in STATE["types"].values() if parent_key(row) == type_key]


def resolve_parent(project_key, own_key, requested, location):
    parent = (requested or "").strip()
    if not parent:
        return ""
    if location or (own_key and own_key == parent):
        return None
    row = STATE["types"].get(parent)
    if not row or row.get("location") or row.get("projectKey") != project_key:
        return None
    if parent_key(row) or (own_key and child_types(own_key)):
        return None
    return row["typeKey"]


def asset_dto(asset, text, with_issues):
    row = type_row(asset["typeKey"]) or {"label": asset["typeKey"], "color": "#5D6B82", "fields": [], "systemType": False, "baseKey": "", "location": False, "projectKey": asset["projectKey"]}
    typed = type_dto(row, text, project_assets(asset["projectKey"])) if asset["typeKey"] in STATE["types"] else {
        "label": asset["typeKey"], "color": "#5D6B82", "icon": default_icon(False), "fields": [], "location": False
    }
    attributes = []
    for field in typed["fields"]:
        attributes.append({
            "fieldKey": field["fieldKey"], "name": field["label"], "kind": field["kind"],
            "required": field["required"],
            "showOnIssue": field["showOnIssue"] if "showOnIssue" in field else True,
            "value": asset["values"].get(field["fieldKey"], ""),
            "displayValue": (USERS.get(asset["values"].get(field["fieldKey"], "")) or {}).get("displayName") or asset["values"].get(field["fieldKey"], "")
            if field["kind"] == "user" else asset["values"].get(field["fieldKey"], ""),
        })
    holder = USERS.get(asset.get("custodianKey") or "")
    dto = {
        "id": asset["id"], "objectKey": asset["objectKey"], "name": asset["name"], "description": asset["description"],
        "typeKey": asset["typeKey"], "typeLabel": typed["label"], "color": typed["color"], "icon": typed["icon"],
        "status": canonical(asset["status"]), "parentId": normalize_parent(asset["parentId"]),
        "sortOrder": asset["sortOrder"], "created": asset["created"], "updated": asset["updated"],
        "createdBy": USERS["ivanov"]["displayName"], "updatedBy": USERS["ivanov"]["displayName"], "projectKey": asset["projectKey"],
        "projectName": next(item["name"] for item in PROJECTS if item["key"] == asset["projectKey"]),
        "location": location_of(asset), "editable": True,
        "cardImage": asset["id"] in STATE["card_images"],
        "custodian": holder, "attributes": attributes,
        "serviceDue": service_due_names(asset["id"]),
        "offeredTypes": list(asset.get("offeredTypes") or []) if is_location_id(asset["id"]) else [],
    }
    if with_issues:
        dto["plans"] = plan_dtos(asset["id"])
        dto["issues"] = [dict(ISSUES_BY_ID[link["issueId"]]) for link in STATE["links"] if link["assetId"] == asset["id"] and link["issueId"] in ISSUES_BY_ID]
        dto["comments"] = [comment_dto(row) for row in comments_of(asset["id"])]
        dto["files"] = [file_dto(row) for row in files_of(asset["id"])]
        dto["activities"] = [activity_dto(row) for row in activities_of(asset["id"])]
    return dto


def now_stamp():
    return datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")


def comments_of(asset_id):
    rows = [row for row in STATE["comments"].values() if row["assetId"] == asset_id]
    rows.sort(key=lambda item: (item["created"], item["id"]))
    return rows


def files_of(asset_id):
    rows = [row for row in STATE["files"].values() if row["assetId"] == asset_id]
    rows.sort(key=lambda item: (item["created"], item["id"]))
    return rows


def comment_dto(row):
    author = USERS.get(row.get("authorKey") or "")
    if not author:
        author = {"userKey": row.get("authorKey") or "", "displayName": row.get("authorKey") or "", "active": False}
    return {"id": row["id"], "body": row["body"], "created": row["created"], "author": author}


def activities_of(asset_id):
    rows = [row for row in STATE["activities"].values() if row["assetId"] == asset_id]
    rows.sort(key=lambda item: (item["created"], item["id"]))
    return rows


def activity_dto(row):
    author_key = row.get("authorKey") or ""
    author = USERS.get(author_key)
    if not author:
        if not author_key:
            author = {"userKey": "", "displayName": "Дерево активов", "active": True}
        else:
            author = {"userKey": author_key, "displayName": author_key, "active": False}
    return {
        "id": row["id"], "action": row["action"], "field": row.get("field") or "",
        "oldValue": row.get("oldValue") or "", "newValue": row.get("newValue") or "",
        "created": row["created"], "author": author,
    }


def record_activity(asset_id, kind, field, old_value, new_value, author="ivanov"):
    activity_id = STATE["activity_seq"]
    STATE["activity_seq"] += 1
    STATE["activities"][activity_id] = {
        "id": activity_id, "assetId": asset_id, "authorKey": author or "",
        "action": kind, "field": field or "", "oldValue": (old_value or "")[:500], "newValue": (new_value or "")[:500],
        "created": now_stamp(),
    }


def log_activity(asset_id, kind, field, old_value, new_value, author="ivanov"):
    left = old_value or ""
    right = new_value or ""
    if left == right:
        return
    record_activity(asset_id, kind, field, left, right, author)


def plan_rows(asset_id):
    rows = [row for row in STATE["plans"].values() if row["assetId"] == asset_id]
    rows.sort(key=lambda item: item["id"])
    return rows


def add_months(day, count):
    month_index = day.month - 1 + count
    year = day.year + month_index // 12
    month = month_index % 12 + 1
    last = calendar.monthrange(year, month)[1]
    return datetime.date(year, month, min(day.day, last))


def parse_plan_date(value):
    text = (value or "").strip()
    for fmt in ("%Y-%m-%d", "%d.%m.%Y"):
        try:
            return datetime.datetime.strptime(text, fmt).date()
        except ValueError:
            continue
    return None


def plan_next(row):
    last = parse_plan_date(row.get("lastDone"))
    count = int(row.get("everyCount") or 0)
    unit = row.get("everyUnit") or ""
    if not last or count < 1:
        return None
    if unit == "once" and count == 1:
        return last
    if unit == "day" and count <= 3650:
        return last + datetime.timedelta(days=count)
    if unit == "week" and count <= 520:
        return last + datetime.timedelta(days=7 * count)
    if unit == "month" and count <= 120:
        return add_months(last, count)
    if unit == "year" and count <= 30:
        return add_months(last, 12 * count)
    return None


def plan_completed(row, nxt):
    applied = row.get("appliedFor") or ""
    if applied == "done":
        return True
    return (row.get("everyUnit") == "once") and bool(nxt) and applied == nxt.isoformat()


def plan_due(row, today=None):
    nxt = plan_next(row)
    if not nxt or plan_completed(row, nxt):
        return False
    return nxt <= (today or datetime.date.today())


def schedule_anchor(first, count, unit):
    parsed = parse_plan_date(first)
    if not parsed or unit == "once":
        return first
    if unit == "day":
        return (parsed - datetime.timedelta(days=count)).isoformat()
    if unit == "week":
        return (parsed - datetime.timedelta(days=7 * count)).isoformat()
    if unit == "year":
        return add_months(parsed, -12 * count).isoformat()
    if unit == "month":
        return add_months(parsed, -count).isoformat()
    return first


def plan_dto(row):
    nxt = plan_next(row)
    completed = plan_completed(row, nxt)
    return {
        "id": row["id"],
        "name": row.get("name") or "",
        "lastDone": row.get("lastDone") or "",
        "everyCount": row.get("everyCount") or 0,
        "everyUnit": row.get("everyUnit") or "",
        "statusKey": row.get("statusKey") or "",
        "notify": bool(row.get("notify", True)),
        "nextDue": nxt.isoformat() if nxt else "",
        "due": plan_due(row),
        "kind": row.get("kind") or "to",
        "assigneeKey": row.get("assigneeKey") or "",
        "assigneeName": (USERS.get(row.get("assigneeKey") or "") or {}).get("displayName") or "",
        "note": row.get("note") or "",
        "startTime": row.get("startTime") or "",
        "endTime": row.get("endTime") or "",
        "completedOn": row.get("completedOn") or "",
        "completed": completed,
    }


def plan_dtos(asset_id):
    return [plan_dto(row) for row in plan_rows(asset_id)]


def schedule_item(plan, asset):
    item = plan_dto(plan)
    item.update({
        "assetId": asset["id"],
        "assetName": asset.get("name") or "",
        "objectKey": asset.get("objectKey") or "",
    })
    return item


def schedule_rows(project_key):
    rows = []
    for plan in STATE["plans"].values():
        asset = STATE["assets"].get(plan["assetId"])
        if not asset or asset.get("projectKey") != project_key or is_location_id(asset["id"]):
            continue
        rows.append(schedule_item(plan, asset))
    return rows


def staff_dto(row):
    user = USERS.get(row.get("userKey") or "") or {}
    return {
        "id": row["id"],
        "userKey": row.get("userKey") or "",
        "userName": user.get("displayName") or row.get("userKey") or "",
        "kind": row.get("kind") or "",
        "weekdays": int(row.get("weekdays") or 0),
        "hours": int(row.get("hours") or 0),
        "startOn": row.get("startOn") or "",
        "endOn": row.get("endOn") or "",
    }


def staff_list(project_key):
    rows = [staff_dto(row) for row in STATE["duties"].values() if row.get("projectKey") == project_key]
    rows.sort(key=lambda item: ((item.get("userName") or "").lower(), 0 if item.get("kind") == "hours" else 1, item.get("startOn") or "", item["id"]))
    return rows


def parse_staff_day(raw):
    text_day = (raw or "").strip()
    if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", text_day):
        return None
    try:
        return datetime.date.fromisoformat(text_day)
    except ValueError:
        return None


def save_staff(project_key, body, text):
    body = body or {}
    user_key = (body.get("userKey") or "").strip()
    if user_key not in USERS:
        return 400, {"message": text["asset-tree.error.user"]}
    try:
        weekdays = int(body.get("weekdays") or 0)
        hours = int(body.get("hours") or 0)
    except (TypeError, ValueError):
        return 400, {"message": text["asset-tree.error.staff"]}
    if weekdays < 1 or weekdays > 127 or hours < 1 or hours > 24:
        return 400, {"message": text["asset-tree.error.staff"]}
    existing = None
    owned = [row for row in STATE["duties"].values() if row.get("projectKey") == project_key]
    for row in owned:
        if row.get("userKey") == user_key and row.get("kind") == "hours":
            existing = row
            break
    if existing is None and len(owned) >= 60:
        return 400, {"message": text["asset-tree.error.staff"]}
    if existing is None:
        duty_id = STATE["duty_seq"]
        STATE["duty_seq"] += 1
        existing = {"id": duty_id, "projectKey": project_key, "userKey": user_key, "kind": "hours"}
        STATE["duties"][duty_id] = existing
    existing["weekdays"] = weekdays
    existing["hours"] = hours
    existing["startOn"] = ""
    existing["endOn"] = ""
    return 200, staff_dto(existing)


def add_staff_leave(project_key, body, text):
    body = body or {}
    user_key = (body.get("userKey") or "").strip()
    if user_key not in USERS:
        return 400, {"message": text["asset-tree.error.user"]}
    start = parse_staff_day(body.get("startOn"))
    end = parse_staff_day(body.get("endOn"))
    if start is None or end is None or end < start or (end - start).days > 366:
        return 400, {"message": text["asset-tree.error.staff"]}
    owned = [row for row in STATE["duties"].values() if row.get("projectKey") == project_key]
    known = False
    leaves = 0
    for row in owned:
        if row.get("userKey") != user_key:
            continue
        if row.get("kind") == "hours":
            known = True
        elif row.get("kind") == "leave":
            leaves += 1
    if not known or leaves >= 24 or len(owned) >= 60:
        return 400, {"message": text["asset-tree.error.staff"]}
    duty_id = STATE["duty_seq"]
    STATE["duty_seq"] += 1
    row = {
        "id": duty_id, "projectKey": project_key, "userKey": user_key, "kind": "leave",
        "weekdays": 0, "hours": 0, "startOn": start.isoformat(), "endOn": end.isoformat(),
    }
    STATE["duties"][duty_id] = row
    return 201, staff_dto(row)


def delete_staff(project_key, duty_id, text):
    row = STATE["duties"].get(duty_id)
    if not row or row.get("projectKey") != project_key:
        return 404, {"message": text["asset-tree.error.notFound"]}
    if row.get("kind") == "hours":
        person = row.get("userKey")
        gone = [item["id"] for item in list(STATE["duties"].values()) if item.get("projectKey") == project_key and item.get("userKey") == person]
        for item_id in gone:
            STATE["duties"].pop(item_id, None)
    else:
        STATE["duties"].pop(duty_id, None)
    return 204, None


def create_schedule(project_key, body, text):
    if not module_on(project_key, "service"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    body = body or {}
    ids = body.get("assetIds") or []
    if not ids or len(ids) > 40:
        return 400, {"message": text.get("asset-tree.error.service", "Schedule")}
    kind = (body.get("kind") or "to").strip().lower()
    labels = {"to": "ТО", "kts": "КТС", "verify": "Поверка"}
    if kind not in labels:
        return 400, {"message": text.get("asset-tree.error.service", "Schedule")}
    name = (body.get("name") or "").strip() or labels[kind]
    staged = []
    for raw in ids:
        asset = STATE["assets"].get(int(raw))
        if not asset or asset.get("projectKey") != project_key or is_location_id(asset["id"]):
            return 400, {"message": text.get("asset-tree.error.service", "Schedule")}
        if len(plan_rows(asset["id"])) >= 12:
            return 400, {"message": text.get("asset-tree.error.service.limit", "Limit")}
        row = {"assetId": asset["id"], "appliedFor": "", "notify": False}
        payload = dict(body)
        payload["name"] = name
        payload["kind"] = kind
        payload["lastDone"] = schedule_anchor(body.get("lastDone"), int(body.get("everyCount") or 0), (body.get("everyUnit") or "").strip().lower())
        error = fill_plan(row, payload, True, project_key, text)
        if error:
            return 400, {"message": error}
        staged.append((row, asset))
    created = []
    for row, asset in staged:
        plan_id = STATE["plan_seq"]
        STATE["plan_seq"] += 1
        row["id"] = plan_id
        STATE["plans"][plan_id] = row
        created.append(schedule_item(row, asset))
    return 201, created


def floor_dto(row):
    rooms = []
    for room in row.get("rooms") or []:
        rooms.append({
            "id": room["id"], "name": room.get("name") or "", "kind": room.get("kind") or "room",
            "color": room_color(room.get("color")), "x": room["x"], "y": room["y"], "w": room["w"], "h": room["h"],
        })
    pins = []
    for pin in row.get("pins") or []:
        asset = STATE["assets"].get(pin.get("assetId"))
        pins.append({
            "id": pin["id"], "assetId": pin.get("assetId") or 0,
            "assetName": (asset or {}).get("name") or "",
            "objectKey": (asset or {}).get("objectKey") or "",
            "x": pin.get("x") or 0, "y": pin.get("y") or 0,
        })
    place = STATE["assets"].get(row.get("placeId"))
    return {
        "id": row["id"], "name": row.get("name") or "", "level": row.get("level") or 1,
        "placeId": row.get("placeId") or 0, "placeName": (place or {}).get("name") or "",
        "planW": plan_limit(row.get("planW"), 1200, 640, 2400),
        "planH": plan_limit(row.get("planH"), 760, 480, 1600),
        "rooms": rooms, "pins": pins,
    }


def project_floors(project_key):
    rows = [row for row in STATE.setdefault("floors", {}).values() if row.get("projectKey") == project_key]
    rows.sort(key=lambda item: (item.get("level") or 0, item["id"]))
    return [floor_dto(row) for row in rows]


def create_floor(project_key, body, text):
    if not module_on(project_key, "map"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    name = ((body or {}).get("name") or "").strip()
    raw_level = (body or {}).get("level")
    level = 1 if raw_level is None or raw_level == "" else int(raw_level)
    if not name or len(name) > 80 or level < -20 or level > 200:
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    if len(project_floors(project_key)) >= 40:
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    place_id = int((body or {}).get("placeId") or 0)
    if place_id and (place_id not in STATE["assets"] or not is_location_id(place_id)):
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    floor_id = STATE["floor_seq"]
    STATE["floor_seq"] += 1
    STATE["floors"][floor_id] = {
        "id": floor_id, "projectKey": project_key, "name": name, "level": level,
        "placeId": place_id, "rooms": [], "pins": [], "room_seq": 1, "pin_seq": 1,
        "planW": plan_limit((body or {}).get("planW"), 1200, 640, 2400),
        "planH": plan_limit((body or {}).get("planH"), 760, 480, 1600),
    }
    return 201, floor_dto(STATE["floors"][floor_id])


def save_floor(project_key, floor_id, body, text):
    if not module_on(project_key, "map"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    row = STATE.setdefault("floors", {}).get(floor_id)
    if not row or row.get("projectKey") != project_key:
        return 404, {"message": text["asset-tree.error.notFound"]}
    body = body or {}
    name = (body.get("name") or "").strip()
    raw_level = body.get("level")
    level = 0 if raw_level is None or raw_level == "" else int(raw_level)
    if not name or len(name) > 80 or level < -20 or level > 200:
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    plan_w = plan_limit(body.get("planW"), 1200, 640, 2400)
    plan_h = plan_limit(body.get("planH"), 760, 480, 1600)
    rooms = body.get("rooms") or []
    pins = body.get("pins") or []
    if len(rooms) > 80 or len(pins) > 200:
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    place_id = int(body.get("placeId") or 0)
    if place_id and (place_id not in STATE["assets"] or not is_location_id(place_id)):
        return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
    stored_rooms = []
    seq = 1
    for room in rooms:
        kind = (room.get("kind") or "room").strip().lower()
        if kind not in ("room", "wall", "door", "window"):
            kind = "room"
        label = (room.get("name") or "").strip()
        if not label and kind == "room":
            label = "Комната"
        minimum = 36 if kind == "room" else 6
        x = max(0, min(plan_w - minimum, int(room.get("x") or 0)))
        y = max(0, min(plan_h - minimum, int(room.get("y") or 0)))
        w = max(minimum, int(room.get("w") or minimum))
        h = max(minimum, int(room.get("h") or minimum))
        if x + w > plan_w:
            w = plan_w - x
        if y + h > plan_h:
            h = plan_h - y
        stored_rooms.append({
            "id": seq, "name": label[:80], "kind": kind, "color": room_color(room.get("color")),
            "x": x, "y": y, "w": w, "h": h,
        })
        seq += 1
    stored_pins = []
    seen = set()
    pin_seq = 1
    for pin in pins:
        asset_id = int(pin.get("assetId") or 0)
        if asset_id in seen:
            continue
        asset = STATE["assets"].get(asset_id)
        if not asset or asset.get("projectKey") != project_key or is_location_id(asset_id):
            return 400, {"message": text.get("asset-tree.error.floor", "Floor")}
        seen.add(asset_id)
        stored_pins.append({
            "id": pin_seq, "assetId": asset_id,
            "x": max(0, min(plan_w, int(pin.get("x") or 0))),
            "y": max(0, min(plan_h, int(pin.get("y") or 0))),
        })
        pin_seq += 1
    row.update({
        "name": name, "level": level, "placeId": place_id, "planW": plan_w, "planH": plan_h,
        "rooms": stored_rooms, "pins": stored_pins,
    })
    return 200, floor_dto(row)


def delete_floor(project_key, floor_id, text):
    row = STATE.setdefault("floors", {}).get(floor_id)
    if not row or row.get("projectKey") != project_key:
        return 404, {"message": text["asset-tree.error.notFound"]}
    if not module_on(project_key, "map"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    STATE["floors"].pop(floor_id, None)
    return 204, None


REPORT_STANDARD = ("status", "type", "place", "holder", "receipts", "cost", "warranty", "age", "writeoff", "work")


def parse_money(raw):
    text = str(raw or "").strip().replace(" ", "").replace("\u00a0", "").replace(",", ".")
    if not text:
        return 0
    if not re.fullmatch(r"\d{1,12}(\.\d{1,2})?", text):
        return None
    rubles, _, fraction = text.partition(".")
    kopecks = (fraction + "00")[:2]
    return int(rubles) * 100 + int(kopecks or "0")


def format_money(minor):
    rubles, kopecks = divmod(int(minor), 100)
    return str(rubles) if kopecks == 0 else "%s.%02d" % (rubles, kopecks)


def life_row(asset_id):
    return STATE.setdefault("lives", {}).get(asset_id)


def life_dto(asset_id):
    stored = life_row(asset_id)
    row = stored or {}
    price = int(row.get("priceMinor") or 0)
    residual = int(row.get("residualMinor") or 0)
    months = int(row.get("lifeMonths") or 0)
    monthly = 0 if months <= 0 or price <= residual else (price - residual) // months
    book = price
    purchased = row.get("purchased") or ""
    if purchased and monthly:
        try:
            start = datetime.date.fromisoformat(purchased)
            today = datetime.date.today()
            elapsed = (today.year - start.year) * 12 + today.month - start.month
            if today.day < start.day:
                elapsed -= 1
            elapsed = max(0, elapsed)
            book = max(residual, price - monthly * elapsed)
        except ValueError:
            book = price
    warranty = row.get("warrantyUntil") or ""
    active = False
    if warranty:
        try:
            active = datetime.date.fromisoformat(warranty) >= datetime.date.today()
        except ValueError:
            active = False
    approver = row.get("approverKey") or ""
    requester = row.get("requesterKey") or ""
    return {
        "assetId": asset_id,
        "purchased": purchased,
        "supplier": row.get("supplier") or "",
        "price": format_money(price) if stored else "",
        "warrantyUntil": warranty,
        "lifeMonths": months,
        "residual": format_money(residual) if stored else "",
        "monthly": format_money(monthly) if stored else "",
        "book": format_money(book) if stored else "",
        "warrantyActive": active,
        "writeoffState": row.get("writeoffState") or "",
        "writeoffReason": row.get("writeoffReason") or "",
        "requesterKey": requester,
        "requesterName": (USERS.get(requester) or {}).get("displayName") or "",
        "approverKey": approver,
        "approverName": (USERS.get(approver) or {}).get("displayName") or "",
        "decidedAt": row.get("decidedAt") or "",
        "document": row.get("document") or "",
        "currency": row.get("currency") or "RUB",
        "commissioned": row.get("commissioned") or "",
        "warrantyService": row.get("warrantyService") or "",
    }


def save_life(asset_id, body, text):
    asset = STATE["assets"].get(asset_id)
    if not asset or is_location_id(asset_id):
        return 400, {"message": text.get("asset-tree.error.life", "Life")}
    if not module_on(asset["projectKey"], "life"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    row = STATE["lives"].setdefault(asset_id, {"assetId": asset_id})
    if row.get("writeoffState") == "approved":
        return 400, {"message": text.get("asset-tree.error.writeoff.closed", "Closed")}
    body = body or {}
    price = parse_money(body.get("price"))
    residual = parse_money(body.get("residual"))
    months = int(body.get("lifeMonths") or 0)
    supplier = (body.get("supplier") or "").strip()
    if price is None or residual is None or months < 0 or months > 600 or len(supplier) > 120 or residual > price:
        return 400, {"message": text.get("asset-tree.error.life", "Life")}
    currency = (body.get("currency") or "RUB").strip().upper() or "RUB"
    document = (body.get("document") or "").strip()
    service = (body.get("warrantyService") or "").strip()
    if len(currency) != 3 or len(document) > 160 or len(service) > 160:
        return 400, {"message": text.get("asset-tree.error.life", "Life")}
    row.update({
        "purchased": (body.get("purchased") or "").strip(),
        "warrantyUntil": (body.get("warrantyUntil") or "").strip(),
        "commissioned": (body.get("commissioned") or "").strip(),
        "supplier": supplier,
        "document": document,
        "currency": currency,
        "warrantyService": service,
        "priceMinor": price,
        "residualMinor": residual,
        "lifeMonths": months,
    })
    if "approverKey" in body and row.get("writeoffState") != "pending":
        approver = str(body.get("approverKey") or "").strip()
        if approver and approver not in USERS:
            return 400, {"message": text.get("asset-tree.error.writeoff.approver", "Approver")}
        row["approverKey"] = approver
    return 200, life_dto(asset_id)


def request_writeoff(asset_id, body, text):
    asset = STATE["assets"].get(asset_id)
    if not asset or is_location_id(asset_id):
        return 400, {"message": text.get("asset-tree.error.life", "Life")}
    if not module_on(asset["projectKey"], "life"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    reason = ((body or {}).get("reason") or "").strip()
    approver = ((body or {}).get("approverKey") or "").strip()
    if not reason or len(reason) > 500:
        return 400, {"message": text.get("asset-tree.error.life", "Life")}
    if approver not in USERS:
        return 400, {"message": text.get("asset-tree.error.writeoff.approver", "Approver")}
    row = STATE["lives"].setdefault(asset_id, {"assetId": asset_id})
    if row.get("writeoffState") in ("pending", "approved"):
        return 400, {"message": text.get("asset-tree.error.writeoff", "Write-off")}
    row.update({
        "writeoffState": "pending", "writeoffReason": reason,
        "requesterKey": "ivanov", "approverKey": approver, "decidedAt": "",
    })
    return 200, life_dto(asset_id)


def decide_writeoff(asset_id, body, text):
    asset = STATE["assets"].get(asset_id)
    row = life_row(asset_id)
    if not asset or not row or row.get("writeoffState") != "pending":
        return 400, {"message": text.get("asset-tree.error.writeoff", "Write-off")}
    if row.get("approverKey") != "ivanov":
        return 403, {"message": text.get("asset-tree.error.writeoff.approver", "Approver")}
    approve = bool((body or {}).get("approve"))
    row["writeoffState"] = "approved" if approve else "rejected"
    row["decidedAt"] = datetime.date.today().isoformat()
    if approve:
        asset["status"] = "written_off"
    return 200, life_dto(asset_id)


def report_group(key):
    if key in ("status", "type", "place", "holder"):
        return key
    return {"cost": "type", "receipts": "month", "age": "age", "warranty": "warranty", "writeoff": "writeoff", "work": "kind"}.get(key, "")


def report_catalog(project_key, text):
    titles = {
        "status": text.get("asset-tree.ui.reportStatusTitle", "Status"),
        "type": text.get("asset-tree.ui.reportTypeTitle", "Type"),
        "place": text.get("asset-tree.ui.reportPlaceTitle", "Place"),
        "holder": text.get("asset-tree.ui.reportHolderTitle", "Custodians"),
        "receipts": text.get("asset-tree.ui.reportReceipts", "Receipts"),
        "warranty": text.get("asset-tree.ui.reportWarrantyTitle", "Warranty"),
        "cost": text.get("asset-tree.ui.reportCostTitle", "Cost"),
        "age": text.get("asset-tree.ui.reportAge", "Age"),
        "writeoff": text.get("asset-tree.ui.reportWriteoffs", "Write-offs"),
        "work": text.get("asset-tree.ui.reportWork", "Work"),
    }
    items = [{"id": key, "name": titles[key], "groupBy": report_group(key), "chart": "donut" if key == "status" else "bar", "metric": "count", "standard": True, "filterField": "", "filterValue": ""} for key in REPORT_STANDARD]
    own = [row for row in STATE.setdefault("saved_reports", {}).values() if row.get("projectKey") == project_key]
    own.sort(key=lambda item: item["id"])
    for row in own:
        items.append({"id": "s%s" % row["id"], "name": row["name"], "groupBy": row["groupBy"], "filterField": row.get("filterField") or "", "filterValue": row.get("filterValue") or "", "chart": row.get("chart") or "bar", "metric": row.get("metric") or "count", "standard": False})
    return items


def create_saved_report(project_key, body, text):
    if not module_on(project_key, "reports"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    body = body or {}
    name = (body.get("name") or "").strip()
    group = (body.get("groupBy") or "").strip().lower()
    filt = (body.get("filterField") or "").strip().lower()
    value = (body.get("filterValue") or "").strip()
    if group not in ("status", "type", "place", "holder") or not name or len(name) > 80:
        return 400, {"message": text.get("asset-tree.error.report", "Report")}
    if filt and filt not in ("status", "type", "holder", "mix"):
        return 400, {"message": text.get("asset-tree.error.report", "Report")}
    if bool(filt) != bool(value) or len(value) > 255:
        return 400, {"message": text.get("asset-tree.error.report", "Report")}
    chart = (body.get("chart") or "bar").strip().lower()
    if chart not in ("bar", "donut"):
        chart = "bar"
    metric = (body.get("metric") or "count").strip().lower()
    if metric not in ("count", "cost"):
        metric = "count"
    report_id = STATE["report_seq"]
    STATE["report_seq"] += 1
    STATE["saved_reports"][report_id] = {
        "id": report_id, "projectKey": project_key, "name": name, "groupBy": group,
        "filterField": filt, "filterValue": value, "chart": chart, "metric": metric,
    }
    return 201, {"id": "s%s" % report_id, "name": name, "groupBy": group, "filterField": filt, "filterValue": value, "chart": chart, "metric": metric, "standard": False}


def delete_saved_report(project_key, report_key, text):
    if not module_on(project_key, "reports"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    if not str(report_key).startswith("s"):
        return 400, {"message": text.get("asset-tree.error.report", "Report")}
    try:
        report_id = int(str(report_key)[1:])
    except ValueError:
        return 400, {"message": text.get("asset-tree.error.report", "Report")}
    row = STATE.setdefault("saved_reports", {}).get(report_id)
    if not row or row.get("projectKey") != project_key:
        return 404, {"message": text["asset-tree.error.notFound"]}
    STATE["saved_reports"].pop(report_id, None)
    return 204, None


def report_view(project_key, report_key, text):
    if not module_on(project_key, "reports"):
        return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
    catalog = {item["id"]: item for item in report_catalog(project_key, text)}
    spec = catalog.get(report_key)
    if not spec:
        return 404, {"message": text["asset-tree.error.notFound"]}
    group = spec.get("groupBy") or "status"
    assets = [asset for asset in project_assets(project_key) if not is_location_id(asset["id"])]
    filt = spec.get("filterField") or ""
    value = spec.get("filterValue") or ""

    def token_hit(raw, token):
        return (token or "") in [part for part in (raw or "").split(",") if part]

    if filt == "status":
        assets = [asset for asset in assets if token_hit(value, asset.get("status") or "")]
    elif filt == "type":
        assets = [asset for asset in assets if token_hit(value, asset.get("typeKey") or "")]
    elif filt == "holder":
        assets = [asset for asset in assets if token_hit(value, asset.get("custodianKey") or "")]
    elif filt == "mix":
        types = ""
        statuses = ""
        for part in value.split(";"):
            if part.startswith("t="):
                types = part[2:]
            elif part.startswith("s="):
                statuses = part[2:]
        assets = [asset for asset in assets if (not types or token_hit(types, asset.get("typeKey") or "")) and (not statuses or token_hit(statuses, asset.get("status") or ""))]
    buckets = {}
    lines = []
    total_book = 0
    for asset in assets:
        if report_key in ("service", "work") and not plan_rows(asset["id"]):
            continue
        life = life_row(asset["id"]) or {}
        if report_key == "warranty" and not life.get("warrantyUntil"):
            continue
        if report_key == "cost" and not life.get("priceMinor"):
            continue
        if report_key in ("receipts", "age") and not life.get("purchased"):
            continue
        if report_key == "writeoff" and asset.get("status") != "written_off" and life.get("writeoffState") != "approved":
            continue
        if report_key in ("service", "work"):
            plan = plan_rows(asset["id"])[0]
            bucket = plan.get("kind") or "to"
            label = {"to": "ТО", "kts": "КТС", "verify": "Поверка"}.get(bucket, bucket)
            detail = label
        elif report_key == "receipts":
            bucket = (life.get("purchased") or "")[:7]
            label = bucket
            detail = bucket
        elif report_key == "age":
            bucket = "y0"
            try:
                start = datetime.date.fromisoformat(life.get("purchased") or "")
                today = datetime.date.today()
                months = (today.year - start.year) * 12 + today.month - start.month
                if months >= 60:
                    bucket = "y5"
                elif months >= 36:
                    bucket = "y3"
                elif months >= 12:
                    bucket = "y1"
            except ValueError:
                bucket = "y0"
            label = text.get("asset-tree.ui.reportAge" + bucket[1:], bucket)
            detail = life.get("purchased") or ""
        elif report_key == "writeoff":
            bucket = (life.get("decidedAt") or "")[:7] or "none"
            label = bucket
            detail = life.get("writeoffReason") or ""
        elif report_key == "warranty":
            bucket = "active"
            label = life.get("warrantyUntil") or ""
            detail = label
        elif report_key == "cost":
            bucket = asset.get("typeKey") or ""
            label = (STATE["types"].get(bucket) or {}).get("label") or bucket
            detail = format_money(life.get("priceMinor") or 0)
            total_book += int(life.get("priceMinor") or 0)
        elif group == "type":
            bucket = asset.get("typeKey") or ""
            label = (STATE["types"].get(bucket) or {}).get("label") or bucket
            detail = ""
        elif group == "holder":
            bucket = asset.get("custodianKey") or ""
            label = (USERS.get(bucket) or {}).get("displayName") or text.get("asset-tree.ui.reportUnassigned", "None")
            detail = ""
        elif group == "place":
            parent = STATE["assets"].get(asset.get("parentId"))
            bucket = str(parent["id"]) if parent else ""
            label = parent.get("name") if parent else text.get("asset-tree.ui.root", "Root")
            detail = ""
        else:
            bucket = asset.get("status") or ""
            label = bucket
            for status_row in ensure_statuses(project_key, text):
                if status_row["statusKey"] == bucket:
                    label = status_row["label"]
                    break
            detail = ""
        color = "#0052CC"
        if report_key == "status" or group == "status":
            color = {"in_use": "#00875A", "active": "#00875A", "repair": "#FF8B00", "maintenance": "#FF8B00", "written_off": "#DE350B", "reserve": "#6554C0", "in_stock": "#6B778C"}.get(bucket, "#0052CC")
        slot = buckets.setdefault(bucket, {"key": bucket, "label": label, "color": color, "count": 0})
        slot["count"] += 1
        if len(lines) < 300:
            lines.append({"assetId": asset["id"], "objectKey": asset.get("objectKey") or "", "name": asset.get("name") or "", "detail": detail})
    return 200, {
        "id": report_key, "title": spec["name"], "hint": text.get("asset-tree.ui.reportHint", ""),
        "total": sum(item["count"] for item in buckets.values()),
        "sum": format_money(total_book) if report_key == "cost" else "",
        "standard": bool(spec.get("standard")),
        "groupBy": group,
        "chart": spec.get("chart") or ("donut" if report_key == "status" else "bar"),
        "rows": list(buckets.values()),
        "lines": lines,
    }


def service_due_names(asset_id):
    names = [row.get("name") or "" for row in plan_rows(asset_id) if plan_due(row)]
    return ", ".join(names)


def apply_due(project_key=None):
    today = datetime.date.today()
    for row in sorted(STATE["plans"].values(), key=lambda item: item["id"]):
        asset = STATE["assets"].get(row["assetId"])
        if not asset or is_location_id(asset["id"]):
            continue
        if project_key and asset["projectKey"] != project_key:
            continue
        if not module_on(asset["projectKey"], "service"):
            continue
        if row.get("appliedFor") == "done":
            continue
        nxt = plan_next(row)
        if not nxt or nxt > today:
            continue
        due_key = nxt.isoformat()
        if row.get("appliedFor") == due_key:
            continue
        status_key = (row.get("statusKey") or "").strip()
        if status_key and status_known(asset["projectKey"], status_key, messages("ru")):
            old = canonical(asset.get("status"))
            if old != status_key:
                asset["status"] = status_key
                asset["updated"] = now_stamp()
                log_activity(asset["id"], "status", "", old, status_key, "")
        record_activity(asset["id"], "service_due", row.get("name") or "", row.get("lastDone") or "", due_key, "")
        row["appliedFor"] = due_key
        if row.get("notify", True):
            person = USERS.get(asset.get("custodianKey") or "")
            email = (person or {}).get("email") or ""
            if email:
                subject = "Подошёл срок %s: %s" % (row.get("name") or "", asset.get("name") or "")
                body = "%s (%s)\n%s: %s -> %s\nhttp://127.0.0.1:47121/plugins/servlet/asset-tree?project=%s&view=all#%s" % (
                    asset.get("name") or "", asset.get("objectKey") or "", row.get("name") or "",
                    row.get("lastDone") or "", due_key, asset.get("projectKey") or "", asset["id"],
                )
                STATE["mails"].append({"to": email, "subject": subject, "body": body, "assetId": asset["id"]})


def fill_plan(row, body, creating, project_key, text):
    body = body or {}
    if body.get("done") is True:
        row["completedOn"] = datetime.date.today().isoformat()
        if row.get("everyUnit") == "once":
            row["appliedFor"] = "done"
        else:
            row["lastDone"] = datetime.date.today().isoformat()
            row["appliedFor"] = ""
    elif body.get("lastDone") is not None or creating:
        parsed = parse_plan_date(body.get("lastDone"))
        if not parsed:
            return text.get("asset-tree.error.service", "Schedule")
        iso = parsed.isoformat()
        if iso != row.get("lastDone"):
            row["lastDone"] = iso
            row["appliedFor"] = ""
    if body.get("name") is not None or creating:
        name = (body.get("name") or "").strip()
        if not name or len(name) > 80:
            return text.get("asset-tree.error.service", "Schedule")
        row["name"] = name
    unit = row.get("everyUnit") or ""
    count = int(row.get("everyCount") or 0)
    if body.get("everyUnit") is not None or creating:
        unit = (body.get("everyUnit") or "").strip().lower()
        if unit not in ("once", "day", "week", "month", "year"):
            unit = ""
    if body.get("everyCount") is not None or creating:
        try:
            count = int(body.get("everyCount") or 0)
        except (TypeError, ValueError):
            count = 0
    allowed = (
        (unit == "once" and count == 1)
        or (unit == "day" and 1 <= count <= 3650)
        or (unit == "week" and 1 <= count <= 520)
        or (unit == "month" and 1 <= count <= 120)
        or (unit == "year" and 1 <= count <= 30)
    )
    if (body.get("everyUnit") is not None or body.get("everyCount") is not None or creating) and not allowed:
        return text.get("asset-tree.error.service", "Schedule")
    if unit != (row.get("everyUnit") or "") or count != int(row.get("everyCount") or 0):
        row["appliedFor"] = ""
    row["everyUnit"] = unit
    row["everyCount"] = count
    if body.get("statusKey") is not None or creating:
        status_key = (body.get("statusKey") or "").strip()
        if status_key and not status_known(project_key, status_key, text):
            return text.get("asset-tree.error.status", "Status")
        row["statusKey"] = status_key
    if body.get("notify") is not None or creating:
        row["notify"] = True if body.get("notify") is None else bool(body.get("notify"))
    if body.get("kind") is not None or creating:
        kind = (body.get("kind") or "to").strip().lower()
        if kind not in ("to", "kts", "verify"):
            return text.get("asset-tree.error.service", "Schedule")
        row["kind"] = kind
    if body.get("assigneeKey") is not None or creating:
        assignee = (body.get("assigneeKey") or "").strip()
        if assignee and assignee not in USERS:
            return text.get("asset-tree.error.user", "User")
        row["assigneeKey"] = assignee
    if body.get("note") is not None or creating:
        note = (body.get("note") or "").strip()
        if len(note) > 2000:
            return text.get("asset-tree.error.service", "Schedule")
        row["note"] = note
    if body.get("startTime") is not None or creating:
        row["startTime"] = (body.get("startTime") or "").strip()
    if body.get("endTime") is not None or creating:
        row["endTime"] = (body.get("endTime") or "").strip()
    if not row.get("name") or not row.get("lastDone") or not row.get("everyUnit"):
        return text.get("asset-tree.error.service", "Schedule")
    return None


def is_location_id(asset_id):
    asset = STATE["assets"].get(asset_id)
    if not asset:
        return False
    return bool((STATE["types"].get(asset["typeKey"]) or {}).get("location"))


def place_container(start_id, skip=None):
    current = normalize_parent(start_id)
    seen = set()
    skipped = skip or set()
    while current and current not in seen:
        seen.add(current)
        asset = STATE["assets"].get(current)
        if current in skipped:
            current = normalize_parent(asset.get("parentId")) if asset else None
            continue
        if is_location_id(current):
            return current
        current = normalize_parent(asset.get("parentId")) if asset else None
    return None


def log_place(place_id, kind, child):
    if not place_id or not child:
        return
    key = child.get("objectKey") or ""
    name = child.get("name") or ""
    if kind in ("place_add", "place_in"):
        record_activity(place_id, kind, key, "", name)
    else:
        record_activity(place_id, kind, key, name, "")


def file_dto(row):
    author = USERS.get(row.get("authorKey") or "")
    return {
        "id": row["id"], "fileName": row["fileName"], "contentType": row["contentType"],
        "size": row["size"], "created": row["created"],
        "authorName": author["displayName"] if author else "",
    }


def safe_file_name(raw):
    name = (raw or "").strip().replace("\\", "/").split("/")[-1]
    if not name or name in (".", ".."):
        return None
    if len(name) > 180:
        name = name[-180:]
    return name


def preview_file_path(file_id):
    return os.path.join(FILE_DIR, str(file_id))


def write_preview_file(file_id, data):
    os.makedirs(FILE_DIR, exist_ok=True)
    with open(preview_file_path(file_id), "wb") as handle:
        handle.write(data)


def delete_preview_file(file_id):
    path = preview_file_path(file_id)
    if os.path.isfile(path):
        os.remove(path)


CARD_IMAGE_MAX = 512 * 1024


def card_image_path(asset_id):
    return os.path.join(FILE_DIR, "card-%s" % asset_id)


def delete_card_image(asset_id):
    path = card_image_path(asset_id)
    if os.path.isfile(path):
        os.remove(path)


def card_image_kind(data):
    if data[:8].startswith(b"\x89PNG"):
        return "png"
    if data[:3] == b"\xff\xd8\xff":
        return "jpeg"
    return ""


def multipart_token(value):
    semi = value.find(";")
    return value[:semi].strip() if semi >= 0 else value.strip()


def multipart_unquote(value):
    if value is None:
        return None
    text = value.strip()
    if text.startswith('"') and text.endswith('"') and len(text) > 1:
        text = text[1:-1]
    return text


def multipart_decode_star(value):
    text = multipart_unquote(value) or ""
    mark = text.find("''")
    if mark >= 0:
        text = text[mark + 2:]
    return unquote(text)


def multipart_filename(headers):
    plain = None
    encoded = None
    for line in re.split(r"\r?\n", headers):
        lower = line.lower()
        if not lower.startswith("content-disposition:"):
            continue
        starred = lower.find("filename*=")
        if starred >= 0:
            encoded = multipart_token(line[starred + len("filename*="):].strip())
        named = lower.find("filename=")
        if named >= 0 and (starred < 0 or named < starred):
            plain = multipart_token(line[named + len("filename="):].strip())
    chosen = multipart_decode_star(encoded) if encoded else multipart_unquote(plain)
    if not chosen:
        return None
    return chosen.replace("\\", "/").split("/")[-1]


def multipart_content_type(headers):
    for line in re.split(r"\r?\n", headers):
        if line.lower().startswith("content-type:"):
            value = line.split(":", 1)[1].strip()
            semi = value.find(";")
            return (value[:semi] if semi >= 0 else value).strip() or "application/octet-stream"
    return "application/octet-stream"


def parse_multipart(body, content_type):
    if not body or not content_type:
        return None
    lowered = content_type.lower()
    at = lowered.find("boundary=")
    if at < 0:
        return None
    boundary = content_type[at + len("boundary="):].strip()
    if boundary.startswith('"') and boundary.endswith('"') and len(boundary) > 1:
        boundary = boundary[1:-1]
    semi = boundary.find(";")
    if semi >= 0:
        boundary = boundary[:semi].strip()
    if not boundary:
        return None
    fence = ("--" + boundary).encode("latin-1", "replace")
    separator = b"\r\n" + fence
    cursor = body.find(fence)
    if cursor < 0:
        return None
    cursor += len(fence)
    while cursor < len(body):
        if cursor + 1 < len(body) and body[cursor:cursor + 2] == b"--":
            break
        if cursor < len(body) and body[cursor:cursor + 1] == b"\r":
            cursor += 2
        elif cursor < len(body) and body[cursor:cursor + 1] == b"\n":
            cursor += 1
        header_end = body.find(b"\r\n\r\n", cursor)
        gap = 4
        if header_end < 0:
            header_end = body.find(b"\n\n", cursor)
            gap = 2
        if header_end < 0:
            return None
        headers = body[cursor:header_end].decode("latin-1", "replace")
        data_start = header_end + gap
        next_at = body.find(separator, data_start)
        data_end = next_at
        step = len(separator)
        if next_at < 0:
            next_at = body.find(fence, data_start)
            data_end = next_at
            step = len(fence)
            if data_end >= 2 and body[data_end - 2:data_end] == b"\r\n":
                data_end -= 2
            elif data_end >= 1 and body[data_end - 1:data_end] == b"\n":
                data_end -= 1
        if data_end < 0:
            data_end = len(body)
            step = 0
            next_at = len(body)
        if data_end < data_start:
            return None
        file_name = multipart_filename(headers)
        if file_name:
            return file_name, multipart_content_type(headers), body[data_start:data_end]
        cursor = next_at + step
    return None


def equipment_headers(text):
    return [
        text.get("asset-tree.ui.keyLabel", "Key"),
        text.get("asset-tree.ui.name", "Name"),
        text.get("asset-tree.ui.type", "Type"),
        text.get("asset-tree.ui.status", "Status"),
        text.get("asset-tree.ui.exchangePlace", "Place"),
        text.get("asset-tree.ui.custodian", "Custodian"),
        text.get("asset-tree.ui.description", "Description"),
    ]


def display_date(value, day_first):
    iso = canonical_date(value)
    if not iso:
        return value or ""
    if not day_first:
        return iso
    year, month, day = iso.split("-")
    return "%s.%s.%s" % (day, month, year)


def equipment_table(project_key, text, day_first):
    ensure_portal_demo()
    headers = equipment_headers(text)
    statuses = {row["statusKey"]: row["label"] for row in status_dtos(project_key, text)}
    types = {row["typeKey"]: row for row in STATE["types"].values() if row["projectKey"] == project_key}
    fields = []
    seen = {header.strip().lower() for header in headers}
    for kind in types.values():
        if kind.get("location"):
            continue
        for field in kind.get("fields") or []:
            label = label_of(field["label"], text)
            marker = label.strip().lower()
            if not marker or marker in seen:
                continue
            seen.add(marker)
            fields.append((field, label))
            headers.append(label)
    assets = [asset for asset in project_assets(project_key) if not (types.get(asset["typeKey"]) or {}).get("location")]
    assets.sort(key=lambda item: item["objectKey"])
    lines = []
    for asset in assets:
        kind = types.get(asset["typeKey"]) or {}
        person = USERS.get(asset.get("custodianKey") or "")
        line = [
            asset["objectKey"],
            asset["name"],
            kind.get("label") or asset["typeKey"],
            statuses.get(canonical(asset.get("status")), asset.get("status") or ""),
            location_of(asset),
            person["displayName"] if person else "",
            asset.get("description") or "",
        ]
        values = asset.get("values") or {}
        for field, _label in fields:
            value = values.get(field["fieldKey"]) or ""
            if field.get("kind") == "date" and value:
                value = display_date(value, day_first)
            line.append(value)
        lines.append(line)
    return headers, lines


def equipment_csv(project_key, text):
    headers, rows = equipment_table(project_key, text, True)
    buffer = StringIO()
    buffer.write("\ufeff")
    writer = csv.writer(buffer, delimiter=";", lineterminator="\n")
    writer.writerow(headers)
    writer.writerows(rows)
    return buffer.getvalue()


def xml_text(value):
    text = value or ""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def column_name(index):
    name = ""
    number = index + 1
    while number:
        number, rem = divmod(number - 1, 26)
        name = chr(65 + rem) + name
    return name


def sheet_title(raw, used=None):
    cleaned = []
    for ch in raw or "":
        if ch in "\\/?*[]:" or ord(ch) < 32:
            if cleaned and cleaned[-1] != " ":
                cleaned.append(" ")
            continue
        if len("".join(cleaned)) >= 31:
            break
        cleaned.append(ch)
    title = "".join(cleaned).strip().strip("'").strip()
    if len(title) > 31:
        title = title[:31].strip()
    if not title:
        title = "Sheet"
    if used is None:
        return title
    candidate = title
    number = 2
    while candidate.lower() in used:
        suffix = " (%d)" % number
        stem = title[:31 - len(suffix)].strip() or "Sheet"
        candidate = stem + suffix
        number += 1
    used.add(candidate.lower())
    return candidate


def sheet_xml(headers, rows):
    sheet = ["<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
             "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"]
    table = [headers] + rows
    for number, cells in enumerate(table, start=1):
        sheet.append("<row r=\"%d\">" % number)
        for index, cell in enumerate(cells):
            sheet.append("<c r=\"%s%d\" t=\"inlineStr\"><is><t xml:space=\"preserve\">%s</t></is></c>"
                         % (column_name(index), number, xml_text(cell)))
        sheet.append("</row>")
    sheet.append("</sheetData></worksheet>")
    return "".join(sheet)


def equipment_pages(project_key, text, day_first):
    ensure_portal_demo()
    common = equipment_headers(text)
    statuses = {row["statusKey"]: row["label"] for row in status_dtos(project_key, text)}
    kinds = [row for row in STATE["types"].values() if row["projectKey"] == project_key and not row.get("location")]
    pages = []
    used = set()
    for kind in kinds:
        headers = list(common)
        seen = {header.strip().lower() for header in headers}
        fields = []
        for field in kind.get("fields") or []:
            label = label_of(field["label"], text)
            marker = label.strip().lower()
            if not marker or marker in seen:
                continue
            seen.add(marker)
            fields.append((field, label))
            headers.append(label)
        assets = [asset for asset in project_assets(project_key) if asset["typeKey"] == kind["typeKey"]]
        assets.sort(key=lambda item: item["objectKey"])
        lines = []
        for asset in assets:
            person = USERS.get(asset.get("custodianKey") or "")
            line = [
                asset["objectKey"],
                asset["name"],
                kind.get("label") or asset["typeKey"],
                statuses.get(canonical(asset.get("status")), asset.get("status") or ""),
                location_of(asset),
                person["displayName"] if person else "",
                asset.get("description") or "",
            ]
            values = asset.get("values") or {}
            for field, _label in fields:
                value = values.get(field["fieldKey"]) or ""
                if field.get("kind") == "date" and value:
                    value = display_date(value, day_first)
                line.append(value)
            lines.append(line)
        pages.append((sheet_title(kind.get("label") or kind["typeKey"], used), headers, lines))
    if not pages:
        pages.append(("Equipment", common, []))
    return pages


def workbook_bytes(pages):
    if not pages:
        pages = [("Equipment", [], [])]
    overrides = []
    sheet_tags = []
    relationships = []
    parts = {}
    for index, page in enumerate(pages, start=1):
        name, headers, rows = page
        overrides.append(
            "<Override PartName=\"/xl/worksheets/sheet%d.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" % index
        )
        sheet_tags.append("<sheet name=\"%s\" sheetId=\"%d\" r:id=\"rId%d\"/>" % (xml_text(name).replace("\"", "&quot;"), index, index))
        relationships.append(
            "<Relationship Id=\"rId%d\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet%d.xml\"/>" % (index, index)
        )
        parts["xl/worksheets/sheet%d.xml" % index] = sheet_xml(headers, rows)
    relationships.append(
        "<Relationship Id=\"rId%d\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" % (len(pages) + 1)
    )
    parts["[Content_Types].xml"] = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
%s
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""" % "\n".join(overrides)
    parts["_rels/.rels"] = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    parts["xl/workbook.xml"] = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>%s</sheets></workbook>""" % "".join(sheet_tags)
    parts["xl/_rels/workbook.xml.rels"] = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
%s
</Relationships>""" % "\n".join(relationships)
    parts["xl/styles.xml"] = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
    buffer = BytesIO()
    with zipfile.ZipFile(buffer, "w", compression=zipfile.ZIP_DEFLATED) as book:
        for name, xml in parts.items():
            book.writestr(name, xml.encode("utf-8"))
    return buffer.getvalue()


def equipment_xlsx(project_key, text, day_first):
    return workbook_bytes(equipment_pages(project_key, text, day_first))


def xml_local(tag):
    return tag.rsplit("}", 1)[-1]


def column_index(ref):
    column = 0
    used = 0
    for ch in ref or "":
        if "A" <= ch <= "Z" or "a" <= ch <= "z":
            column = column * 26 + (ord(ch.upper()) - 64)
            used += 1
        else:
            break
    return column - 1 if used else -1


def plain_number(raw):
    try:
        value = float(raw)
    except ValueError:
        return raw
    if abs(value) < 1e15 and abs(value - round(value)) < 1e-6:
        return str(int(round(value)))
    return raw


def excel_date(raw):
    try:
        days = int(float(raw))
        return (datetime.date(1970, 1, 1) + datetime.timedelta(days=days - 25569)).isoformat()
    except (ValueError, OverflowError):
        return raw


def workbook_targets(book, names):
    if "xl/workbook.xml" not in names or "xl/_rels/workbook.xml.rels" not in names:
        return []
    workbook = ET.fromstring(book.read(names["xl/workbook.xml"]))
    rels = ET.fromstring(book.read(names["xl/_rels/workbook.xml.rels"]))
    targets = {}
    for node in rels.iter():
        if xml_local(node.tag) != "Relationship":
            continue
        target = (node.attrib.get("Target") or "").replace("\\", "/").lstrip("/")
        if target and not target.startswith("xl/"):
            target = "xl/" + target
        targets[node.attrib.get("Id")] = target
    found = []
    for node in workbook.iter():
        if xml_local(node.tag) != "sheet":
            continue
        relation = ""
        for key, value in node.attrib.items():
            if xml_local(key) == "id":
                relation = value
        path = targets.get(relation)
        if path and path in names:
            found.append((node.attrib.get("name") or "", names[path]))
    return found


def parse_sheet_xml(root, shared):
    headers = []
    records = []
    for row in root.iter():
        if xml_local(row.tag) != "row":
            continue
        cells = {}
        next_column = 0
        for cell in list(row):
            if xml_local(cell.tag) != "c":
                continue
            column = column_index(cell.attrib.get("r"))
            if column < 0:
                column = next_column
            cells[column] = workbook_cell(cell, shared)
            next_column = column + 1
        if not cells:
            continue
        line = [cells.get(index, "") for index in range(max(cells) + 1)]
        if not any(item.strip() for item in line):
            continue
        if not headers:
            headers = [item.strip() for item in line]
        else:
            try:
                number = int(row.attrib.get("r") or 0)
            except ValueError:
                number = 0
            records.append((number or len(records) + 2, line))
    return headers, records


def parse_workbook(data):
    with zipfile.ZipFile(BytesIO(data)) as book:
        names = {}
        for name in book.namelist():
            names[name.replace("\\", "/").lstrip("/")] = name
        shared = []
        if "xl/sharedStrings.xml" in names:
            root = ET.fromstring(book.read(names["xl/sharedStrings.xml"]))
            for node in root:
                if xml_local(node.tag) != "si":
                    continue
                parts = []
                for child in node.iter():
                    if xml_local(child.tag) == "t" and child.text:
                        parts.append(child.text)
                shared.append("".join(parts))
        targets = workbook_targets(book, names)
        if not targets:
            sheet_name = names.get("xl/worksheets/sheet1.xml")
            if not sheet_name:
                for key, original in names.items():
                    if key.startswith("xl/worksheets/") and key.endswith(".xml"):
                        sheet_name = original
                        break
            if not sheet_name:
                raise ValueError("sheet")
            targets = [("", sheet_name)]
        pages = []
        for title, original in targets:
            headers, records = parse_sheet_xml(ET.fromstring(book.read(original)), shared)
            pages.append((title, headers, records))
    if not pages:
        raise ValueError("sheet")
    return pages


def workbook_cell(cell, shared):
    kind = cell.attrib.get("t")
    texts = []
    value = ""
    for node in cell.iter():
        name = xml_local(node.tag)
        if name == "t" and node.text:
            texts.append(node.text)
        elif name == "v" and node.text:
            value = node.text.strip()
    if kind == "inlineStr" or (texts and kind != "s" and not value):
        return "".join(texts)
    if kind == "s":
        try:
            index = int(value)
        except ValueError:
            return ""
        return shared[index] if 0 <= index < len(shared) else ""
    if kind == "b":
        return "true" if value == "1" else "false"
    if kind == "str" or not value:
        return value or "".join(texts)
    return plain_number(value)


def parse_sheet(source):
    text = source or ""
    if text.startswith("\ufeff"):
        text = text[1:]
    sample = text.splitlines()[0] if text.splitlines() else ""
    delimiter = ";" if sample.count(";") >= sample.count(",") and ";" in sample else ","
    rows = list(csv.reader(StringIO(text), delimiter=delimiter))
    while rows and not any(cell.strip() for cell in rows[0]):
        rows.pop(0)
    if not rows:
        return [], []
    headers = [cell.strip() for cell in rows[0]]
    records = []
    for index, cells in enumerate(rows[1:], start=2):
        if any(cell.strip() for cell in cells):
            records.append((index, cells))
    return headers, records


def sheet_role(header):
    name = " ".join((header or "").strip().lower().replace("ё", "е").split())
    roles = {
        "key": "key", "ключ": "key",
        "name": "name", "название": "name", "имя": "name",
        "type": "type", "тип": "type",
        "status": "status", "статус": "status",
        "place": "place", "площадка": "place", "место": "place",
        "custodian": "custodian", "ответственный": "custodian", "мол": "custodian",
        "description": "description", "описание": "description",
    }
    return roles.get(name)


def find_preview_user(raw):
    text = (raw or "").strip().lower()
    if not text:
        return None
    matches = []
    for user in USERS.values():
        if text in {user["userKey"].lower(), user["username"].lower(), user["email"].lower(), user["displayName"].lower()}:
            matches.append(user["userKey"])
    if len(matches) == 1:
        return matches[0]
    return ""


def find_preview_place(project_key, path):
    parts = [part.strip() for part in (path or "").split("/") if part.strip()]
    if not parts:
        return None
    parent = None
    types = {row["typeKey"]: row for row in STATE["types"].values()}
    for part in parts:
        matches = []
        for asset in project_assets(project_key):
            kind = types.get(asset["typeKey"]) or {}
            if not kind.get("location") or asset["name"].lower() != part.lower():
                continue
            if normalize_parent(asset["parentId"]) == parent:
                matches.append(asset)
        if len(matches) != 1:
            return None
        parent = matches[0]["id"]
    return parent


def import_request(project_key, body, text):
    body = body or {}
    content = body.get("content") or ""
    if content:
        try:
            data = base64.b64decode(content)
        except Exception:
            message = text.get("asset-tree.error.import.workbook", "Workbook")
            return {"created": 0, "updated": 0, "errors": [{"row": 1, "message": message}]}
        name = (body.get("name") or "").lower()
        if data[:4] == b"\xd0\xcf\x11\xe0" or (name.endswith(".xls") and not name.endswith(".xlsx")):
            message = text.get("asset-tree.error.import.workbook", "Workbook")
            return {"created": 0, "updated": 0, "errors": [{"row": 1, "message": message}]}
        if data[:2] == b"PK":
            try:
                pages = parse_workbook(data)
            except Exception:
                message = text.get("asset-tree.error.import.workbook", "Workbook")
                return {"created": 0, "updated": 0, "errors": [{"row": 1, "message": message}]}
            return import_book(project_key, pages, text)
        source = data.decode("utf-8", "replace")
    else:
        source = body.get("csv") or ""
    headers, records = parse_sheet(source)
    return import_rows(project_key, headers, records, text)


def import_equipment(project_key, source, text):
    headers, records = parse_sheet(source)
    return import_rows(project_key, headers, records, text)


def import_book(project_key, pages, text):
    types = [row for row in STATE["types"].values() if row["projectKey"] == project_key and not row.get("location")]
    total = 0
    for _name, _headers, records in pages:
        total += len(records)
    if total > 2000:
        message = text.get("asset-tree.error.import.limit", "Limit").replace("{0}", "2000")
        return {"created": 0, "updated": 0, "errors": [{"row": 1, "message": message}]}
    created = 0
    updated = 0
    errors = []
    seen = set()
    any_rows = False
    for name, headers, records in pages:
        if not headers and not records:
            continue
        bound = bound_sheet_type(name, types)
        headers, records, rejected = bind_sheet_type(name, headers, records, bound, text)
        errors.extend(rejected)
        if not records:
            continue
        any_rows = True
        result = import_rows(project_key, headers, records, text, seen, name)
        created += result["created"]
        updated += result["updated"]
        errors.extend(result["errors"])
    if not any_rows and not errors:
        return {"created": 0, "updated": 0, "errors": [{"row": 1, "message": text.get("asset-tree.error.import.empty", "Empty")}]}
    return {"created": created, "updated": updated, "errors": errors}


def bound_sheet_type(name, types):
    marker = " ".join((name or "").strip().lower().replace("ё", "е").split())
    if not marker:
        return None
    hits = []
    for kind in types:
        label = kind.get("label") or ""
        names = {
            " ".join(label.strip().lower().replace("ё", "е").split()),
            " ".join((kind.get("typeKey") or "").strip().lower().replace("ё", "е").split()),
            " ".join(sheet_title(label).strip().lower().replace("ё", "е").split()),
        }
        if marker in names:
            hits.append(kind)
    return hits[0] if len(hits) == 1 else None


def bind_sheet_type(name, headers, records, bound, text):
    if not bound:
        return headers, records, []
    type_index = None
    for index, header in enumerate(headers):
        if sheet_role(header) == "type":
            type_index = index
            break
    label = bound.get("label") or bound.get("typeKey") or ""
    keys = {label.strip().lower(), (bound.get("typeKey") or "").strip().lower()}
    prepared = []
    errors = []
    message = text.get("asset-tree.error.import.sheet.type", "Type \"{0}\" does not belong on this sheet.")
    for row_number, cells in records:
        current = ""
        if type_index is not None and type_index < len(cells):
            current = cells[type_index].strip()
        if current and current.lower() not in keys:
            note = message.replace("{0}", current)
            if name:
                note = "%s: %s" % (name, note)
            errors.append({"row": row_number, "message": note})
            continue
        copied = list(cells)
        if not current:
            if type_index is None:
                copied.append(label)
            else:
                while len(copied) <= type_index:
                    copied.append("")
                copied[type_index] = label
        prepared.append((row_number, copied))
    if type_index is None:
        headers = list(headers) + ["Type"]
    return headers, prepared, errors


def import_rows(project_key, headers, records, text, seen=None, sheet=""):
    def finish(result):
        if sheet:
            for error in result["errors"]:
                error["message"] = "%s: %s" % (sheet, error["message"])
        return result

    roles = {}
    for index, header in enumerate(headers):
        role = sheet_role(header)
        if role:
            roles[role] = index
    if "name" not in roles:
        return finish({"created": 0, "updated": 0, "errors": [{"row": 1, "message": text.get("asset-tree.error.import.header", "Header")}]})
    if not records:
        return finish({"created": 0, "updated": 0, "errors": [{"row": 1, "message": text.get("asset-tree.error.import.empty", "Empty")}]})
    types = [row for row in STATE["types"].values() if row["projectKey"] == project_key]
    statuses = status_dtos(project_key, text)
    by_key = {asset["objectKey"].lower(): asset for asset in project_assets(project_key)}
    created = 0
    updated = 0
    errors = []
    if seen is None:
        seen = set()

    def cell(cells, role):
        index = roles.get(role)
        if index is None or index >= len(cells):
            return ""
        return cells[index].strip()

    for row_number, cells in records:
        name = cell(cells, "name")
        key = cell(cells, "key")
        type_name = cell(cells, "type")
        status_name = cell(cells, "status")
        place = cell(cells, "place")
        custodian_name = cell(cells, "custodian")
        description = cell(cells, "description")
        has_custodian = "custodian" in roles
        has_description = "description" in roles
        if not name:
            errors.append({"row": row_number, "message": text.get("asset-tree.error.import.name", "Name")})
            continue
        existing = None
        if key:
            marker = key.lower()
            if marker in seen:
                errors.append({"row": row_number, "message": text.get("asset-tree.error.import.key.duplicate", "Duplicate").replace("{0}", key)})
                continue
            seen.add(marker)
            existing = by_key.get(marker)
            if not existing:
                errors.append({"row": row_number, "message": text.get("asset-tree.error.import.key", "Key").replace("{0}", key)})
                continue
        kind = None
        for item in types:
            if not item.get("location") and type_name.lower() in {item["label"].lower(), item["typeKey"].lower()}:
                kind = item
                break
        if not kind:
            errors.append({"row": row_number, "message": text.get("asset-tree.error.import.type", "Type").replace("{0}", type_name)})
            continue
        status_key = ""
        if not status_name:
            status_key = "in_use"
        else:
            for status in statuses:
                if status_name.lower() in {status["label"].lower(), status["statusKey"].lower()}:
                    status_key = status["statusKey"]
                    break
        if not status_key:
            errors.append({"row": row_number, "message": text.get("asset-tree.error.import.status", "Status").replace("{0}", status_name)})
            continue
        parent = find_preview_place(project_key, place)
        if not parent:
            message = text.get("asset-tree.error.import.place.required" if not place else "asset-tree.error.import.place", "Place")
            errors.append({"row": row_number, "message": message.replace("{0}", place)})
            continue
        custodian = None
        if custodian_name:
            custodian = find_preview_user(custodian_name)
            if not custodian:
                errors.append({"row": row_number, "message": text.get("asset-tree.error.import.user", "User").replace("{0}", custodian_name)})
                continue
        if existing:
            old_parent = normalize_parent(existing.get("parentId"))
            existing["name"] = name
            existing["typeKey"] = kind["typeKey"]
            existing["status"] = status_key
            existing["parentId"] = parent
            existing["updated"] = now_stamp()
            if has_description:
                existing["description"] = description
            if has_custodian:
                existing["custodianKey"] = custodian
            if old_parent != parent:
                from_place = place_container(old_parent)
                to_place = place_container(parent)
                old_name = STATE["assets"].get(old_parent, {}).get("name", "") if old_parent else ""
                new_name = STATE["assets"].get(parent, {}).get("name", "") if parent else ""
                log_activity(existing["id"], "move", "", old_name, new_name)
                if from_place and from_place != to_place:
                    log_place(from_place, "place_out", existing)
                if to_place and to_place != from_place:
                    log_place(to_place, "place_in", existing)
            updated += 1
        else:
            if not asset_prefix(project_key):
                errors.append({"row": row_number, "message": text.get("asset-tree.error.assetKey.required", "Key")})
                continue
            asset_id = add_asset(project_key, kind["typeKey"], name, parent, status_key, custodian, {})
            if not asset_id:
                errors.append({"row": row_number, "message": text.get("asset-tree.error.assetKey.required", "Key")})
                continue
            STATE["assets"][asset_id]["description"] = description
            by_key[STATE["assets"][asset_id]["objectKey"].lower()] = STATE["assets"][asset_id]
            log_activity(asset_id, "created", "", "", name)
            log_place(place_container(parent), "place_add", STATE["assets"][asset_id])
            created += 1
    return finish({"created": created, "updated": updated, "errors": errors})


def bulk_parents(project_key):
    return {asset["id"]: normalize_parent(asset.get("parentId")) for asset in project_assets(project_key)}


def bulk_roots(ids, parents):
    selected = set(ids)
    roots = []
    for item in ids:
        parent = parents.get(item)
        seen = set()
        covered = False
        while parent:
            if parent in selected:
                covered = True
                break
            if parent in seen:
                break
            seen.add(parent)
            parent = parents.get(parent)
        if not covered:
            roots.append(item)
    return roots


def bulk_depth(item, parents):
    level = 0
    parent = parents.get(item)
    seen = set()
    while parent and parent not in seen:
        seen.add(parent)
        level += 1
        parent = parents.get(parent)
    return level


def bulk_under(item, ancestor, parents):
    parent = parents.get(item)
    seen = set()
    while parent:
        if parent == ancestor:
            return True
        if parent in seen:
            return False
        seen.add(parent)
        parent = parents.get(parent)
    return False


def bulk_apply(project_key, body, text):
    body = body or {}
    action = (body.get("action") or "").strip()
    target = (body.get("target") or "").strip()
    if target == "type":
        return bulk_types(project_key, action, body.get("keys") or [], text)
    if target != "asset" or action not in ("delete", "move", "status", "custodian", "copy"):
        return 400, {"message": text.get("asset-tree.error.bulk.action", "Action")}
    ids = []
    seen = set()
    for raw in body.get("ids") or []:
        try:
            item = int(raw)
        except (TypeError, ValueError):
            continue
        if item > 0 and item not in seen:
            seen.add(item)
            ids.append(item)
    if not ids:
        return 400, {"message": text.get("asset-tree.error.bulk.empty", "Empty")}
    if len(ids) > 200:
        return 400, {"message": text.get("asset-tree.error.bulk.limit", "Limit").replace("{0}", "200")}
    if action == "move" and not body.get("toRoot") and not normalize_parent(body.get("parentId")):
        return 400, {"message": text.get("asset-tree.error.import.place.required", "Place")}
    parents = bulk_parents(project_key)
    outermost = action == "move" or (action == "delete" and body.get("cascade"))
    order = bulk_roots(ids, parents) if outermost else sorted(ids, key=lambda item: -bulk_depth(item, parents))
    covered = set()
    errors = []
    for item in order:
        asset = STATE["assets"].get(item)
        label = asset["name"] if asset else str(item)
        if asset and asset["projectKey"] != project_key:
            errors.append({"label": label, "message": text.get("asset-tree.error.project.mismatch", "Project")})
            continue
        if not asset:
            if outermost:
                covered.add(item)
            continue
        kind = next((row for row in STATE["types"].values() if row["typeKey"] == asset["typeKey"]), {})
        if action in ("status", "custodian") and kind.get("location"):
            errors.append({"label": label, "message": text.get("asset-tree.error.bulk.place", "Place").replace("{0}", label)})
            continue
        if action == "delete":
            status, payload = Handler.delete_asset(Handler, item, bool(body.get("cascade")), text)
            if status >= 400:
                errors.append({"label": label, "message": (payload or {}).get("message") or ""})
                continue
        elif action == "move":
            move = {"parentId": None if body.get("toRoot") else body.get("parentId")}
            status, payload = Handler.move_asset(Handler, item, move, text)
            if status >= 400:
                errors.append({"label": label, "message": (payload or {}).get("message") or ""})
                continue
        elif action == "status":
            asset["status"] = canonical(body.get("status") or "in_use")
            asset["updated"] = now_stamp()
        elif action == "copy":
            if kind.get("location"):
                errors.append({"label": label, "message": text.get("asset-tree.error.copy", "Only equipment can be copied.")})
                continue
            status, payload = Handler.copy_asset(Handler, item, text)
            if status >= 400:
                errors.append({"label": label, "message": (payload or {}).get("message") or ""})
                continue
        else:
            key = (body.get("custodianKey") or "").strip()
            asset["custodianKey"] = key or None
            asset["updated"] = now_stamp()
        covered.add(item)
        if outermost:
            for other in ids:
                if bulk_under(other, item, parents):
                    covered.add(other)
    return 200, {"done": len(covered), "errors": errors}


def bulk_types(project_key, action, keys, text):
    if action != "delete":
        return 400, {"message": text.get("asset-tree.error.bulk.action", "Action")}
    unique = []
    seen = set()
    for key in keys:
        name = (key or "").strip()
        if name and name not in seen:
            seen.add(name)
            unique.append(name)
    if not unique:
        return 400, {"message": text.get("asset-tree.error.bulk.empty", "Empty")}
    done = 0
    errors = []
    for key in unique:
        row = STATE["types"].get(key)
        label = row["label"] if row else key
        if not row:
            errors.append({"label": label, "message": text.get("asset-tree.error.type.notFound", "Type")})
            continue
        if row.get("projectKey") != project_key:
            errors.append({"label": label, "message": text.get("asset-tree.error.project.mismatch", "Project")})
            continue
        if row.get("systemType"):
            errors.append({"label": label, "message": text.get("asset-tree.error.type.system", "System")})
            continue
        if any(asset["typeKey"] == key for asset in STATE["assets"].values()):
            errors.append({"label": label, "message": text.get("asset-tree.error.type.inUse", "Used")})
            continue
        if child_types(key):
            errors.append({"label": label, "message": text.get("asset-tree.error.type.hasSubtypes", "Subtypes")})
            continue
        del STATE["types"][key]
        done += 1
    return 200, {"done": done, "errors": errors}


def line_key(line):
    text = (line or "").strip()
    if "@" in text:
        text = text.split("@", 1)[0].strip()
    return text


def lines_ordered(lines):
    any_line = False
    for line in lines or []:
        text = (line or "").strip()
        if not text:
            continue
        any_line = True
        if "@" not in text:
            return False
    return any_line


def place_facts(project_key):
    rows = [row for row in STATE["types"].values() if row.get("projectKey") == project_key and not row.get("location")]
    rows.sort(key=lambda row: (row.get("sortOrder") or 0, row.get("typeKey") or ""))
    return rows


def place_layout(lines, pinned, facts):
    known = {}
    schema_keys = []
    for row in facts or []:
        key = row.get("typeKey") or ""
        if not key or key in known:
            continue
        known[key] = row
        schema_keys.append(key)
    line_pos = {}
    explicit = {}
    position = 0
    for line in lines or []:
        text = (line or "").strip()
        if not text:
            continue
        key = line_key(text)
        if not key or key not in known or key in line_pos:
            continue
        line_pos[key] = position
        position += 1
        if "@" in text:
            parent = text.split("@", 1)[1].strip()
            if parent == key or parent not in known:
                parent = ""
            explicit[key] = parent
    visible = {}
    for key in pinned or []:
        key = (key or "").strip()
        if key in known:
            visible[key] = True
    for key in line_pos:
        visible[key] = True

    def raw_parent(key):
        if key in explicit:
            return explicit[key]
        schema_parent = parent_key(known[key])
        if schema_parent and schema_parent != key and schema_parent in known:
            return schema_parent
        return ""

    raw = {}

    def remember():
        raw.clear()
        for key in visible:
            raw[key] = raw_parent(key)

    remember()
    for _guard in range(8):
        grew = False
        for key, parent in list(raw.items()):
            if parent and parent not in visible and parent in known:
                visible[parent] = True
                grew = True
        if not grew:
            break
        remember()
    parent_of = {}
    for key in visible:
        parent = raw.get(key) or ""
        parent_of[key] = parent if parent and parent != key and parent in visible else ""
    for _guard in range(8):
        changed = False
        for key, parent in list(parent_of.items()):
            if parent and parent_of.get(parent):
                parent_of[key] = ""
                changed = True
        if not changed:
            break
    roots = []
    children = {}
    for key in schema_keys:
        if key not in visible:
            continue
        parent = parent_of.get(key) or ""
        if not parent or parent not in visible:
            roots.append(key)
            continue
        children.setdefault(parent, []).append(key)

    def score(key):
        if key in line_pos:
            return line_pos[key]
        try:
            at = schema_keys.index(key)
        except ValueError:
            at = len(schema_keys)
        return 1000000 + at

    if lines_ordered(lines):
        roots.sort(key=score)
        for bucket in children.values():
            bucket.sort(key=score)
    slots = []
    for root in roots:
        slots.append({"typeKey": root, "parentKey": ""})
        for child in children.get(root, []):
            slots.append({"typeKey": child, "parentKey": root})
    return slots


def place_move(lines, pinned, facts, type_key, action):
    key = (type_key or "").strip()
    step = (action or "").strip().lower()
    verb = step
    target_key = ""
    if ":" in step:
        verb, target_key = step.split(":", 1)
        target_key = target_key.strip()
    step_move = verb in ("up", "down", "nest", "lift")
    place_move_flag = verb in ("before", "after", "into") and bool(target_key)
    if not key or not (step_move or place_move_flag):
        return None
    slots = place_layout(lines, pinned, facts)
    roots = []
    children = {}
    parent_of = {}
    for slot in slots:
        if not slot["parentKey"]:
            roots.append(slot["typeKey"])
            parent_of[slot["typeKey"]] = ""
            continue
        children.setdefault(slot["parentKey"], []).append(slot["typeKey"])
        parent_of[slot["typeKey"]] = slot["parentKey"]
    if key not in parent_of:
        return None
    parent = parent_of[key]

    def write():
        stored = []
        for root in roots:
            stored.append(root + "@")
            for child in children.get(root, []):
                stored.append(child + "@" + root)
        return stored

    def detach():
        if key in roots:
            roots.remove(key)
        for bucket in children.values():
            if key in bucket:
                bucket.remove(key)

    if place_move_flag:
        if target_key == key or target_key not in parent_of:
            return None
        target_parent = parent_of.get(target_key) or ""
        has_kids = bool(children.get(key))
        if verb == "into":
            if target_parent or has_kids:
                return None
        elif has_kids and target_parent:
            return None
        detach()
        if verb == "into":
            children.setdefault(target_key, []).append(key)
            return write()
        siblings = roots if not target_parent else children.setdefault(target_parent, [])
        at = siblings.index(target_key) if target_key in siblings else -1
        if at < 0:
            siblings.append(key)
        else:
            if verb == "after":
                at += 1
            siblings.insert(at, key)
        return write()
    if verb in ("up", "down"):
        siblings = roots if not parent else children.get(parent) or []
        if key not in siblings:
            return None
        index = siblings.index(key)
        swap = index - 1 if verb == "up" else index + 1
        if swap < 0 or swap >= len(siblings):
            return write()
        siblings[index], siblings[swap] = siblings[swap], siblings[index]
        return write()
    if verb == "nest":
        if parent:
            return None
        if children.get(key):
            return None
        index = roots.index(key) if key in roots else -1
        if index <= 0:
            return None
        previous = roots[index - 1]
        roots.pop(index)
        children.setdefault(previous, []).append(key)
        return write()
    if not parent:
        return None
    nested = children.get(parent) or []
    if key in nested:
        nested.remove(key)
    at = roots.index(parent) if parent in roots else -1
    if at < 0:
        roots.append(key)
    else:
        roots.insert(at + 1, key)
    return write()


def add_pin(lines, facts, type_key):
    known = {}
    for row in facts or []:
        key = row.get("typeKey") or ""
        if key and key not in known:
            known[key] = row
    key = (type_key or "").strip()
    if key not in known:
        return None
    nxt = list(lines or [])
    if any(line_key(line) == key for line in nxt):
        return nxt
    if lines_ordered(nxt):
        parent = parent_key(known[key])
        if parent not in known or parent == key:
            parent = ""
        nxt.append(key + "@" + parent)
    else:
        nxt.append(key)
    return nxt


def remove_pin(lines, type_key):
    key = (type_key or "").strip()
    return [line for line in (lines or []) if line_key(line) != key]


def flow_link_allowed(source, target):
    if source["kind"] == "request":
        return target["kind"] in ("issue", "status")
    return source["kind"] == "issue" and target["kind"] == "status"


def normalize_flow_book(raw):
    processes = []
    seen = set()
    incoming = (raw or {}).get("processes")
    if not isinstance(incoming, list):
        graph = normalize_flow_graph(raw or {})
        return {"processes": [{"id": "legacy", "name": "", "nodes": graph["nodes"], "links": graph["links"]}]}
    for item in incoming:
        node_id = (item.get("id") or "").strip().lower()
        if not re.fullmatch(r"[a-z0-9]{4,16}", node_id) or node_id in seen:
            continue
        seen.add(node_id)
        graph = normalize_flow_graph(item)
        name = " ".join((item.get("name") or "").split())
        if len(name) > 80:
            name = name[:80]
        removed = (item.get("removedStatus") or "").strip()
        if not re.fullmatch(r"[a-z0-9_-]{1,40}", removed):
            removed = ""
        processes.append({"id": node_id, "name": name, "removedStatus": removed, "nodes": graph["nodes"], "links": graph["links"]})
        if len(processes) >= 20:
            break
    return {"processes": processes}


def flow_book_named(book):
    processes = []
    for process in (book or {}).get("processes") or []:
        named = flow_graph_named(process)
        named["id"] = process.get("id") or ""
        named["name"] = process.get("name") or ""
        named["removedStatus"] = process.get("removedStatus") or ""
        processes.append(named)
    return {"processes": processes}


def removed_status_for(project_key, issue_type_id):
    stored = STATE["flow_graphs"].get(project_key)
    if not stored or "processes" not in stored:
        return None
    for process in normalize_flow_book(stored)["processes"]:
        status = process.get("removedStatus") or ""
        if not status:
            continue
        types = [node.get("issueTypeId") for node in process["nodes"] if node.get("kind") == "request"]
        if not types or str(issue_type_id) in [str(item) for item in types]:
            return status
    return None


def apply_removed_flow(asset_id, issue, status_before=""):
    asset = STATE["assets"].get(asset_id)
    if not asset or not issue or is_location_id(asset_id):
        return
    status = removed_status_for(asset.get("projectKey"), issue.get("issueTypeId") or ISSUE_TYPES[0]["id"])
    if status == "_restore":
        status = status_before
    if not status or status == asset.get("status"):
        return
    old = asset.get("status") or ""
    asset["status"] = status
    log_activity(asset_id, "status", "", old, status)


def flow_rules_from_book(project_key, book, names):
    chosen = {}
    for process in (book or {}).get("processes") or []:
        for rule in flow_rules_from_graph(project_key, process, names):
            if rule["issueTypeId"] not in chosen:
                chosen[rule["issueTypeId"]] = rule
    return list(chosen.values())


def normalize_flow_graph(raw):
    nodes = []
    seen = set()
    for item in (raw or {}).get("nodes") or []:
        kind = item.get("kind")
        node_id = (item.get("id") or "").strip().lower()
        if kind not in ("request", "issue", "status") or not re.fullmatch(r"[a-z0-9]{4,16}", node_id) or node_id in seen:
            continue
        seen.add(node_id)
        node = {
            "id": node_id,
            "kind": kind,
            "x": max(0, min(2400, int(item.get("x") or 0))),
            "y": max(0, min(2400, int(item.get("y") or 0))),
            "issueTypeId": (item.get("issueTypeId") or "").strip() if kind == "request" else "",
            "issueStatusId": (item.get("issueStatusId") or "").strip() if kind == "issue" else "",
            "statusKey": (item.get("statusKey") or "").strip() if kind == "status" else "",
        }
        nodes.append(node)
        if len(nodes) >= 40:
            break
    by_id = {node["id"]: node for node in nodes}
    links = []
    seen_links = set()
    for item in (raw or {}).get("links") or []:
        source = by_id.get((item.get("from") or "").strip().lower())
        target = by_id.get((item.get("to") or "").strip().lower())
        if not source or not target or source["id"] == target["id"] or not flow_link_allowed(source, target):
            continue
        key = (source["id"], target["id"])
        if key in seen_links:
            continue
        seen_links.add(key)
        links.append({"from": source["id"], "to": target["id"]})
        if len(links) >= 80:
            break
    return {"nodes": nodes, "links": links}


def flow_graph_named(graph):
    names = {item["id"]: item["name"] for item in ISSUE_TYPES}
    statuses = {item["id"]: item["name"] for item in ISSUE_STATUSES}
    nodes = []
    for node in graph.get("nodes") or []:
        copy = dict(node)
        if copy.get("kind") == "request":
            copy["issueTypeName"] = names.get(copy.get("issueTypeId") or "", "")
        if copy.get("kind") == "issue":
            copy["issueStatusName"] = statuses.get(copy.get("issueStatusId") or "", "")
        nodes.append(copy)
    return {"nodes": nodes, "links": [dict(link) for link in graph.get("links") or []]}


def flow_graph_from_rules(project_key):
    graph = {"nodes": [], "links": []}
    for index, rule in enumerate(STATE["flows"].get(project_key, [])):
        request_id = "req%s" % rule["id"]
        status_id = "sta%s" % rule["id"]
        graph["nodes"].append({
            "id": request_id, "kind": "request", "x": 48, "y": 36 + index * 140,
            "issueTypeId": rule["issueTypeId"], "issueTypeName": rule["issueTypeName"], "statusKey": "",
        })
        graph["nodes"].append({
            "id": status_id, "kind": "status", "x": 420, "y": 36 + index * 140,
            "issueTypeId": "", "statusKey": rule["statusKey"],
        })
        graph["links"].append({"from": request_id, "to": status_id})
    return graph


def flow_rules_from_graph(project_key, graph, names):
    nodes = {node["id"]: node for node in graph["nodes"]}
    chosen = {}
    for link in graph["links"]:
        source = nodes.get(link["from"])
        target = nodes.get(link["to"])
        if not source or not target or source.get("kind") != "request" or target.get("kind") != "status":
            continue
        type_id = source.get("issueTypeId") or ""
        status = target.get("statusKey") or ""
        if type_id and status:
            chosen[type_id] = status
    rules = []
    for type_id, status in chosen.items():
        rule_id = STATE["flow_seq"]
        STATE["flow_seq"] += 1
        rules.append({
            "id": rule_id,
            "issueTypeId": type_id,
            "issueTypeName": names.get(type_id, type_id),
            "statusKey": status,
        })
    return rules


class Handler(BaseHTTPRequestHandler):
    server_version = "AssetTreePreview/1.1"

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

    def do_GET(self):
        self.route("GET")

    def do_POST(self):
        self.route("POST")

    def do_PUT(self):
        self.route("PUT")

    def do_DELETE(self):
        self.route("DELETE")

    def route(self, method):
        parsed = urlparse(self.path)
        path = unquote(parsed.path)
        query = parse_qs(parsed.query)
        if path == "/preview/mails" and method == "GET":
            return self.respond(200, json.dumps(STATE["mails"]).encode("utf-8"), "application/json; charset=utf-8")
        if path in ("/", "/plugins/servlet/asset-tree"):
            return self.serve_page(query)
        if path == "/issue":
            return self.serve_issue(query)
        if path == "/portal" or re.fullmatch(r"/servicedesk/customer/portal/\d+/create/\d+", path):
            return self.serve_portal(path)
        if path.startswith("/rest/servicedeskapi/portals/") or path.startswith("/rest/servicedeskapi/servicedesk/"):
            body = b'{"id":"7","projectId":"10000","projectKey":"TEST","projectName":"Test"}'
            return self.respond(200, body, "application/json; charset=utf-8")
        if path.startswith("/browse/"):
            return self.serve_browse(path.split("/", 2)[2])
        if path.startswith("/download/resources/"):
            return self.serve_static(path.rsplit("/", 1)[-1])
        if path.startswith("/rest/asset-tree/1.0/"):
            rest = path[len("/rest/asset-tree/1.0"):]
            if method == "GET" and re.fullmatch(r"/projects/[A-Za-z0-9]+/equipment\.xlsx", rest):
                project_key = rest.split("/")[2]
                body = equipment_xlsx(project_key, self.text(), self.lang() != "en")
                return self.respond(200, body, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", {
                    "Content-Disposition": 'attachment; filename="equipment-%s.xlsx"' % project_key,
                })
            if method == "GET" and re.fullmatch(r"/projects/[A-Za-z0-9]+/equipment\.csv", rest):
                project_key = rest.split("/")[2]
                body = equipment_csv(project_key, self.text()).encode("utf-8")
                return self.respond(200, body, "text/csv; charset=utf-8", {
                    "Content-Disposition": 'attachment; filename="equipment-%s.csv"' % project_key,
                })
            return self.serve_api(method, rest, query)
        if path == "/plugins/servlet/asset-tree-file":
            return self.serve_file(method, query)
        if path == "/plugins/servlet/asset-tree-card-image":
            return self.serve_card_image(method, query)
        self.send_error(404)

    def lang(self):
        header = self.headers.get("Accept-Language", "ru")
        return "en" if header.lower().startswith("en") else "ru"

    def text(self):
        return messages(self.lang())

    def serve_page(self, query):
        template = open(os.path.join(RES, "templates", "page.html"), encoding="utf-8").read()
        text = self.text()
        project = (query.get("project") or [""])[0]
        view = (query.get("view") or ["all"])[0]
        if view == "dashboard":
            view = "all"
        if view not in ("all", "mine", "search", "settings", "schedules", "reports", "map"):
            view = "all"
        html = (template
                .replace("@@LANG@@", "ru" if self.lang() == "ru" else "en")
                .replace("@@TITLE@@", text.get("asset-tree.ui.title", "Asset tree"))
                .replace("@@CSS@@", "/download/resources/asset-tree/asset-tree.css")
                .replace("@@QR@@", "/download/resources/asset-tree/qr-code.js")
                .replace("@@JS@@", "/download/resources/asset-tree/asset-tree.js")
                .replace("@@REST@@", "/rest/asset-tree/1.0")
                .replace("@@PROJECT@@", project)
                .replace("@@VIEW@@", view))
        html = html.replace("<body>", "<body>\n" + self.preview_header(text, project), 1)
        self.respond(200, html.encode("utf-8"), "text/html; charset=utf-8")

    def preview_header(self, text, project):
        def href(view):
            query = "view=" + view
            if project:
                query = "project=" + project + "&" + query
            return "/plugins/servlet/asset-tree?" + query
        items = [
            ("mine", text.get("asset-tree.nav.mine", "My assets")),
            ("all", text.get("asset-tree.nav.all", "All assets")),
            ("settings", text.get("asset-tree.nav.settings", "Settings")),
        ]
        links = "".join('<a href="%s">%s</a>' % (href(key), label) for key, label in items)
        label = text.get("asset-tree.nav.label", "Assets")
        return """<style>
.preview-top { display: flex; align-items: center; gap: 8px; height: 56px; padding: 0 16px; background: #0747a6; color: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
.preview-brand { font-weight: 600; margin-right: 16px; }
.preview-drop { position: relative; }
.preview-drop > a { color: #fff; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; padding: 8px 10px; border-radius: 3px; }
.preview-drop > a:hover, .preview-drop:focus-within > a { background: rgba(255,255,255,0.16); }
.preview-menu { display: none; position: absolute; top: 100%%; left: 0; min-width: 196px; margin-top: 4px; padding: 4px 0; background: #fff; color: #172b4d; border-radius: 3px; box-shadow: 0 4px 8px -2px rgba(9,30,66,.25), 0 0 1px rgba(9,30,66,.31); z-index: 40; }
.preview-drop:hover .preview-menu, .preview-drop:focus-within .preview-menu { display: block; }
.preview-menu a { display: block; padding: 8px 16px; color: #172b4d; text-decoration: none; }
.preview-menu a:hover { background: #f4f5f7; color: #0052cc; }
</style>
<header class="preview-top"><span class="preview-brand">Jira</span>
<div class="preview-drop"><a href="%s">%s <span aria-hidden="true">▾</span></a>
<div class="preview-menu">%s</div></div></header>""" % (href("all"), label, links)

    def serve_issue(self, query):
        key = ((query or {}).get("key") or ["SD-14"])[0].upper()
        issue = ISSUES.get(key) or ISSUES["SD-14"]
        # late=1 mimics Jira: the panel HTML arrives after the scripts, with no issue id on the element.
        late = ((query or {}).get("late") or ["0"])[0] == "1"
        if late:
            html = """<!DOCTYPE html>
<html lang="ru"><head><meta charset="utf-8"><title>%s</title>
<link rel="stylesheet" href="/download/resources/asset-tree/issue-panel.css">
<style>
body { margin: 0; background: #f4f5f7; font-family: "Segoe UI", sans-serif; }
main { max-width: 880px; margin: 32px auto; display: grid; grid-template-columns: 1fr 280px; gap: 16px; }
article, aside { background: white; border: 1px solid #e3e7ee; border-radius: 12px; padding: 16px; }
h1 { margin: 0 0 8px; font-size: 22px; }
h2 { margin: 0 0 8px; font-size: 14px; }
p { color: #5d6b82; }
</style></head>
<body><main>
<article><h1>%s</h1><p>Панель справа показывает активы только проекта этой заявки.</p></article>
<aside><h2>Активы</h2><div id="panel-slot"></div></aside></main>
<script>
window.JIRA = window.JIRA || {};
JIRA.Events = { NEW_CONTENT_ADDED: 'newContentAdded' };
JIRA._handlers = {};
JIRA.bind = function (name, fn) { (this._handlers[name] = this._handlers[name] || []).push(fn); };
JIRA.trigger = function (name, context) {
  var handlers = this._handlers[name] || [];
  for (var i = 0; i < handlers.length; i++) handlers[i]({ type: name }, context, 'panelRefreshed');
};
JIRA.Issue = { getIssueId: function () { return %s; }, getIssueKey: function () { return '%s'; } };
</script>
<script src="/download/resources/asset-tree/issue-panel.js"></script>
<script>
setTimeout(function () {
  var slot = document.getElementById('panel-slot');
  slot.innerHTML = '<div id="asset-tree-panel" class="asset-tree-panel" data-issue-id="" data-issue-key=""></div>';
  JIRA.trigger(JIRA.Events.NEW_CONTENT_ADDED, slot);
}, 900);
</script>
</body></html>""" % (issue["issueKey"], issue["issueKey"], issue["issueId"], issue["issueKey"])
        else:
            html = """<!DOCTYPE html>
<html lang="ru"><head><meta charset="utf-8"><title>%s</title>
<link rel="stylesheet" href="/download/resources/asset-tree/issue-panel.css">
<style>
body { margin: 0; background: #f4f5f7; font-family: "Segoe UI", sans-serif; }
main { max-width: 880px; margin: 32px auto; display: grid; grid-template-columns: 1fr 280px; gap: 16px; }
article, aside { background: white; border: 1px solid #e3e7ee; border-radius: 12px; padding: 16px; }
h1 { margin: 0 0 8px; font-size: 22px; }
h2 { margin: 0 0 8px; font-size: 14px; }
p { color: #5d6b82; }
</style></head>
<body><main>
<article><h1>%s</h1><p>Панель справа показывает активы только проекта этой заявки.</p></article>
<aside><h2>Активы</h2>
<div id="asset-tree-panel" class="asset-tree-panel" data-issue-id="%s" data-issue-key="%s"></div>
</aside></main>
<script src="/download/resources/asset-tree/issue-panel.js"></script>
</body></html>""" % (issue["issueKey"], issue["issueKey"], issue["issueId"], issue["issueKey"])
        self.respond(200, html.encode("utf-8"), "text/html; charset=utf-8")

    def serve_portal(self, path):
        ensure_portal_demo()
        # The script is in the head, before the form exists, and the asset input
        # appears later. Project key is not in the page: portal 7 answers only
        # through the Service Desk portal API.
        rewrite = ""
        if path == "/portal":
            rewrite = "<script>history.replaceState(null,'','/servicedesk/customer/portal/7/create/5');</script>"
        html = """<!DOCTYPE html>
<html lang="ru"><head><meta charset="utf-8"><title>Техническая поддержка</title>
<link rel="stylesheet" href="/download/resources/asset-tree/asset-field.css">
<style>
body { margin: 0; background: #f4f5f7; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: #172b4d; }
main { max-width: 720px; margin: 32px auto; background: white; border: 1px solid #dfe1e6; border-radius: 3px; padding: 20px; }
form { display: flex; flex-direction: column; gap: 16px; }
label { display: block; margin: 0 0 4px; font-weight: 600; font-size: 12px; color: #6b778c; }
label span { font-weight: 400; }
.field-group { display: flex; flex-direction: column; align-items: flex-start; max-width: 100%%; }
.field-group > select {
    -webkit-appearance: none;
    -moz-appearance: none;
    appearance: none;
    box-sizing: border-box;
    width: 500px;
    max-width: 100%%;
    height: 30px;
    margin: 0;
    padding: 0 32px 0 8px;
    border: 2px solid transparent;
    border-radius: 3px;
    background-color: #ebecf0;
    background-image: url("data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24'%%3E%%3Cpath fill='%%23172B4D' d='M8.292 10.293a1.009 1.009 0 0 0 0 1.419l2.939 2.965c.218.215.5.322.779.322s.556-.107.769-.322l2.93-2.955a1.01 1.01 0 0 0 0-1.419.987.987 0 0 0-1.406 0l-2.298 2.317-2.307-2.327a.99.99 0 0 0-1.406 0z'/%%3E%%3C/svg%%3E");
    background-repeat: no-repeat;
    background-position: right 8px center;
    background-size: 16px 16px;
    color: #172b4d;
    font-size: 14px;
    font-weight: 400;
    line-height: 20px;
    cursor: pointer;
}
form.vp-request-form input[type="text"] {
    box-sizing: border-box;
    width: 100%%;
    height: 40px;
    border: 2px solid #dfe1e6;
    border-radius: 3px;
    padding: 0 8px;
    background: #fafbfc;
    font-size: 14px;
    font-weight: 400;
}
.select2-container { position: relative; display: inline-block; box-sizing: border-box; vertical-align: middle; }
.select2-container .select2-choice { display: flex; align-items: center; box-sizing: border-box; height: 30px; padding: 0 28px 0 8px; border: 2px solid transparent; border-radius: 3px; background: #ebecf0 url("data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24'%%3E%%3Cpath fill='%%23172B4D' d='M8.292 10.293a1 1 0 0 0 0 1.4l2.94 2.97a1 1 0 0 0 1.4 0l2.93-2.96a1 1 0 0 0-1.4-1.41L12 12.59l-2.3-2.33a1 1 0 0 0-1.41 0z'/%%3E%%3C/svg%%3E") no-repeat right 8px center; background-size: 16px 16px; color: #172b4d; font-size: 14px; line-height: 26px; text-decoration: none; cursor: pointer; }
.select2-container.select2-dropdown-open .select2-choice { background-color: #344563; background-image: url("data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24'%%3E%%3Cpath fill='%%23ffffff' d='M8.292 10.293a1 1 0 0 0 0 1.4l2.94 2.97a1 1 0 0 0 1.4 0l2.93-2.96a1 1 0 0 0-1.4-1.41L12 12.59l-2.3-2.33a1 1 0 0 0-1.41 0z'/%%3E%%3C/svg%%3E"); color: #fff; }
.select2-chosen { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.select2-offscreen { position: absolute !important; width: 1px !important; height: 1px !important; padding: 0 !important; margin: -1px !important; overflow: hidden !important; clip: rect(0 0 0 0) !important; border: 0 !important; }
form.vp-request-form .select2-container .select2-focusser { position: absolute !important; left: 0 !important; top: 0 !important; width: 250px !important; height: 100%% !important; max-width: 250px !important; margin: 0 !important; padding: 0 !important; overflow: visible !important; clip: auto !important; border: 0 !important; background: #dadbe2 !important; z-index: 2 !important; }
form.vp-request-form .select2-container .select2-chosen { position: relative; z-index: 3; display: block !important; max-width: 250px !important; background: #dadbe2 !important; }
.select2-drop { position: absolute; left: 0; width: 190px; z-index: 20; background: #fff; border: 1px solid #dfe1e6; border-radius: 0 0 3px 3px; box-shadow: 0 4px 8px rgba(9, 30, 66, 0.25); }
.select2-search { padding: 4px; }
.select2-search input { box-sizing: border-box; width: 160px; height: 28px; margin: 0; padding: 0 28px 0 8px; border: 1px solid #dfe1e6; border-radius: 3px; background: #fff url("data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24'%%3E%%3Cpath fill='%%236B778C' d='M16.4 15l3.8 3.8-1.4 1.4-3.8-3.8a7 7 0 1 1 1.4-1.4zM10 15a5 5 0 1 0 0-10 5 5 0 0 0 0 10z'/%%3E%%3C/svg%%3E") no-repeat right 6px center; background-size: 16px 16px; font-size: 14px; }
.select2-results { list-style: none; margin: 0; padding: 0; max-height: 220px; overflow: auto; }
.select2-result { padding: 6px 8px; color: #172b4d; font-size: 14px; cursor: pointer; }
.select2-result.is-highlighted { background: #ebecf0; }
</style>
%s
<script>
(function () {
    function Wrapper(nodes) { this.nodes = nodes; }
    Wrapper.prototype.data = function (key, value) {
        var el = this.nodes[0];
        if (!el) return undefined;
        el.__auiData = el.__auiData || {};
        if (arguments.length < 2) return el.__auiData[key];
        el.__auiData[key] = value;
        return this;
    };
    Wrapper.prototype.on = function (name, fn) {
        var el = this.nodes[0];
        if (!el) return this;
        el.addEventListener(name, function (event) { fn.call(el, event); });
        return this;
    };
    Wrapper.prototype.auiSelect2 = function (options) {
        var el = this.nodes[0];
        if (!el) return this;
        if (options === 'destroy') { destroySelect2(el); return this; }
        buildSelect2(el, options || {});
        return this;
    };
    function query(arg) {
        var nodes = [];
        if (typeof arg === 'string') nodes = Array.prototype.slice.call(document.querySelectorAll(arg));
        else if (arg && arg.nodeType === 1) nodes = [arg];
        return new Wrapper(nodes);
    }
    query.fn = { auiSelect2: function () {} };
    function destroySelect2(el) {
        if (el.__auiDrop && el.__auiDrop.parentNode) el.__auiDrop.parentNode.removeChild(el.__auiDrop);
        if (el.__auiBox && el.__auiBox.parentNode) el.__auiBox.parentNode.removeChild(el.__auiBox);
        el.__auiDrop = null;
        el.__auiBox = null;
        el.className = (el.className || '').replace(/\\bselect2-offscreen\\b/g, '').replace(/\\s+/g, ' ');
        el.__auiData = {};
    }
    function chosenText(el) {
        var option = el.options[el.selectedIndex];
        return option ? option.text : '';
    }
    function buildSelect2(el, options) {
        destroySelect2(el);
        if ((' ' + el.className + ' ').indexOf(' select2-offscreen ') === -1) el.className += ' select2-offscreen';
        var box = document.createElement('div');
        box.className = 'select2-container' + (options.containerCssClass ? ' ' + options.containerCssClass : '');
        if (el.id) box.id = 's2id_' + el.id;
        if (options.width) box.style.width = options.width;
        var choice = document.createElement('a');
        choice.href = '#';
        choice.className = 'select2-choice';
        var chosen = document.createElement('span');
        chosen.className = 'select2-chosen';
        chosen.textContent = chosenText(el);
        choice.appendChild(chosen);
        box.appendChild(choice);
        var focusser = document.createElement('input');
        focusser.type = 'text';
        focusser.className = 'select2-focusser select2-offscreen';
        box.appendChild(focusser);
        var drop = document.createElement('div');
        drop.className = 'select2-drop' + (options.dropdownCssClass ? ' ' + options.dropdownCssClass : '');
        drop.hidden = true;
        var search = document.createElement('div');
        search.className = 'select2-search';
        var input = document.createElement('input');
        input.type = 'text';
        input.setAttribute('autocomplete', 'off');
        search.appendChild(input);
        var list = document.createElement('ul');
        list.className = 'select2-results';
        drop.appendChild(search);
        drop.appendChild(list);
        box.appendChild(drop);
        el.parentNode.insertBefore(box, el.nextSibling);
        el.__auiBox = box;
        el.__auiDrop = drop;
        function paint() {
            list.innerHTML = '';
            var needle = (input.value || '').toLowerCase();
            var showSearch = options.minimumResultsForSearch === undefined || options.minimumResultsForSearch >= 0;
            search.hidden = !showSearch;
            for (var i = 0; i < el.options.length; i++) {
                var text = el.options[i].text || '';
                if (needle && text.toLowerCase().indexOf(needle) === -1) continue;
                var item = document.createElement('li');
                item.className = 'select2-result';
                item.textContent = text;
                item.setAttribute('data-value', el.options[i].value);
                list.appendChild(item);
            }
        }
        function close() {
            drop.hidden = true;
            box.className = box.className.replace(/\\bselect2-dropdown-open\\b/g, '').replace(/\\s+/g, ' ');
        }
        function shrink() {
            drop.style.width = '190px';
            input.style.width = '160px';
        }
        function open() {
            if (el.disabled) return;
            drop.hidden = false;
            if ((' ' + box.className + ' ').indexOf(' select2-dropdown-open ') === -1) box.className += ' select2-dropdown-open';
            input.value = '';
            paint();
            var opened = document.createEvent('Event');
            opened.initEvent('select2-open', true, false);
            el.dispatchEvent(opened);
            shrink();
            var api = el.__auiData && el.__auiData.select2;
            if (api && typeof api.positionDropdown === 'function') api.positionDropdown();
            window.setTimeout(shrink, 0);
            input.focus();
        }
        choice.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            if (drop.hidden) open(); else close();
        });
        input.addEventListener('input', paint);
        list.addEventListener('click', function (event) {
            var item = event.target;
            if (!item || !item.getAttribute) return;
            var value = item.getAttribute('data-value');
            if (value === null) return;
            el.value = value;
            chosen.textContent = item.textContent;
            close();
            var change = document.createEvent('HTMLEvents');
            change.initEvent('change', true, false);
            el.dispatchEvent(change);
        });
        list.addEventListener('mouseover', function (event) {
            var rows = list.querySelectorAll('.select2-result');
            for (var i = 0; i < rows.length; i++) rows[i].className = 'select2-result';
            if (event.target && event.target.className === 'select2-result') event.target.className = 'select2-result is-highlighted';
        });
        document.addEventListener('click', function (event) {
            if (!box.contains(event.target)) close();
        });
        el.__auiData = el.__auiData || {};
        var containerApi = {
            0: box,
            length: 1,
            width: function (value) { if (value) box.style.width = typeof value === 'number' ? value + 'px' : value; return box.getBoundingClientRect().width; }
        };
        var dropdownApi = { 0: drop, length: 1 };
        el.__auiData.select2 = {
            opts: options,
            container: containerApi,
            dropdown: dropdownApi,
            positionDropdown: shrink
        };
    }
    window.AJS = window.AJS || {};
    window.AJS.$ = query;
    document.addEventListener('DOMContentLoaded', function () {
        var nodes = document.querySelectorAll('.field-group > select, .field-group .field-value > select');
        for (var i = 0; i < nodes.length; i++) query(nodes[i]).auiSelect2({ minimumResultsForSearch: 0, width: '500px' });
    });
})();
</script>
<script src="/download/resources/asset-tree/asset-field.js"></script>
</head>
<body><main>
<h1>%s</h1>
<p>%s</p>
<form id="request-form" class="vp-request-form">
<select id="customfield_10099" class="hidden" style="display:none" aria-hidden="true"><option value="-1">Не выбрано</option></select>
<div class="field-group">
<label>Площадка <span class="vp-optional">(необязательно)</span></label>
<select id="customfield_10100" name="customfield_10100">
<option value="">Не выбрано</option>
<option value="10122" selected>Пункт А</option>
<option value="10123">Пункт Б</option>
</select>
</div>
<div class="field-group">
<label>Отделение</label>
<div class="field-value">
<select id="customfield_10101" name="customfield_10101">
<option value="">Не выбрано</option>
<option value="10130">Пункт А</option>
</select>
</div>
</div>
<div id="asset-slot" class="field-group"></div>
</form>
</main>
<script>
setTimeout(function () {
  var slot = document.getElementById('asset-slot');
  slot.innerHTML = '<label for="customfield_10001">Актив <span>(необязательно)</span></label><input type="text" id="customfield_10001" name="customfield_10001" value="">';
}, 900);
</script>
</body></html>""" % (
            rewrite,
            self.text().get("asset-tree.ui.portalTitle", "Asset"),
            self.text().get("asset-tree.ui.portalHint", ""),
        )
        self.respond(200, html.encode("utf-8"), "text/html; charset=utf-8")

    def serve_browse(self, key):
        issue = ISSUES.get(key.upper())
        if not issue:
            self.send_error(404)
            return
        body = "<h1>%s</h1><p>%s</p><p>%s</p>" % (issue["issueKey"], issue["summary"], issue["status"])
        self.respond(200, body.encode("utf-8"), "text/html; charset=utf-8")

    def serve_static(self, name):
        folder = "css" if name.endswith(".css") else "js" if name.endswith(".js") else None
        if not folder:
            self.send_error(404)
            return
        file_path = os.path.join(RES, folder, name)
        if not os.path.isfile(file_path):
            self.send_error(404)
            return
        content_type = "text/css; charset=utf-8" if name.endswith(".css") else "application/javascript; charset=utf-8"
        with open(file_path, "rb") as handle:
            self.respond(200, handle.read(), content_type)

    def serve_api(self, method, path, query):
        try:
            with LOCK:
                seed()
                status, payload = self.api(method, path, query)
            if status == 204:
                self.send_response(204)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.respond(status, body, "application/json; charset=utf-8")
        except Exception as error:  # noqa: BLE001
            print("preview error", error)
            self.respond(500, json.dumps({"message": "Error"}).encode("utf-8"), "application/json; charset=utf-8")

    def api(self, method, path, query):
        text = self.text()
        if path == "/meta" and method == "GET":
            i18n = {key[len("asset-tree.ui."):]: value for key, value in text.items() if key.startswith("asset-tree.ui.")}
            return 200, {
                "canEdit": True, "canConfigure": True, "canGrant": True, "version": "1.3.23",
                "baseUrl": "http://127.0.0.1:47121",
                "locale": "ru-RU" if self.lang() == "ru" else "en-US",
                "displayName": USERS["ivanov"]["displayName"],
                "userKey": "ivanov", "i18n": i18n,
                "projects": [dict(project_rights(), key=item["key"], name=item["name"], card=card_for(item["key"]), assetKey=asset_prefix(item["key"]), **project_view(item["key"])) for item in PROJECTS],
            }
        if path == "/assets" and method == "GET":
            project_key = (query.get("projectKey") or [""])[0]
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            needle = (query.get("q") or [""])[0].strip().lower()
            apply_due(project_key)
            rows = project_assets(project_key)
            if needle:
                rows = [row for row in rows if needle in (row["name"] + row["objectKey"] + location_of(row)).lower()][:30]
            rows.sort(key=lambda item: (item["sortOrder"], item["id"]))
            return 200, {
                "assets": [asset_dto(row, text, False) for row in rows],
                "types": [type_dto(row, text, project_assets(project_key)) for row in self.project_types(project_key)],
                "statuses": status_dtos(project_key, text),
            }
        if path == "/assets" and method == "POST":
            return self.create_asset(self.read_json(), text)
        if path == "/users" and method == "GET":
            needle = (query.get("q") or [""])[0].strip().lower()
            found = [user for user in USERS.values() if needle and needle in (user["displayName"] + user["email"] + user["department"]).lower()]
            return 200, found[:8]
        match = re.fullmatch(r"/users/([a-z0-9_-]+)/assets", path)
        if match and method == "GET":
            user_key = match.group(1)
            if user_key not in USERS:
                return 404, {"message": text["asset-tree.error.user"]}
            result = []
            for asset in STATE["assets"].values():
                if is_location_id(asset["id"]):
                    continue
                role = text["asset-tree.ui.custodian"] if asset.get("custodianKey") == user_key else None
                if role:
                    dto = asset_dto(asset, text, False)
                    dto["holderRole"] = role
                    result.append(dto)
            result.sort(key=lambda item: ((item.get("projectName") or "").lower(), (item.get("name") or "").lower()))
            return 200, result
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/inventory", path)
        if match and method == "GET":
            return 200, self.inventory(match.group(1), text)
        match = re.fullmatch(r"/assets/(\d+)/inventory", path)
        if match and method == "POST":
            return self.mark_inventory(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/bulk", path)
        if match and method == "POST":
            if match.group(1) not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            return bulk_apply(match.group(1), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/equipment", path)
        if match and method == "POST":
            if match.group(1) not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            body = self.read_json() or {}
            return 200, import_request(match.group(1), body, text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/schedule", path)
        if match:
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "service"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            if method == "GET":
                return 200, schedule_rows(project_key)
            if method == "POST":
                return create_schedule(project_key, self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/staff", path)
        if match:
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "service"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            if method == "GET":
                return 200, staff_list(project_key)
            if method == "PUT":
                return save_staff(project_key, self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/staff/leave", path)
        if match and method == "POST":
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "service"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            return add_staff_leave(project_key, self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/staff/(\d+)", path)
        if match and method == "DELETE":
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "service"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            return delete_staff(project_key, int(match.group(2)), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/floors", path)
        if match:
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "map"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            if method == "GET":
                return 200, project_floors(project_key)
            if method == "POST":
                return create_floor(project_key, self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/floors/(\d+)", path)
        if match:
            project_key = match.group(1)
            floor_id = int(match.group(2))
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if method == "PUT":
                return save_floor(project_key, floor_id, self.read_json(), text)
            if method == "DELETE":
                return delete_floor(project_key, floor_id, text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/reports", path)
        if match:
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if not module_on(project_key, "reports"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            if method == "GET":
                return 200, report_catalog(project_key, text)
            if method == "POST":
                return create_saved_report(project_key, self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/reports/([A-Za-z0-9]+)", path)
        if match:
            project_key = match.group(1)
            report_key = match.group(2)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            if method == "GET":
                return report_view(project_key, report_key, text)
            if method == "DELETE":
                return delete_saved_report(project_key, report_key, text)
        match = re.fullmatch(r"/assets/(\d+)/lifecycle", path)
        if match:
            asset_id = int(match.group(1))
            asset = STATE["assets"].get(asset_id)
            if not asset:
                return 404, {"message": text["asset-tree.error.notFound"]}
            if is_location_id(asset_id):
                return 400, {"message": text.get("asset-tree.error.life", "Life")}
            if not module_on(asset["projectKey"], "life"):
                return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
            if method == "GET":
                return 200, life_dto(asset_id)
            if method == "PUT":
                return save_life(asset_id, self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/lifecycle/writeoff", path)
        if match and method == "POST":
            return request_writeoff(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/lifecycle/decision", path)
        if match and method == "POST":
            return decide_writeoff(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/report", path)
        if match and method == "GET":
            return 200, self.report(match.group(1), text)
        if path == "/asset-fields" and method == "GET":
            return 200, {"fields": ["customfield_10001"]}
        match = re.fullmatch(r"/portals/(\d+)", path)
        if match and method == "GET":
            if match.group(1) == "7":
                return 404, {"message": text["asset-tree.error.project.notFound"]}
            return 200, {"projectKey": "TEST"}
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/asset-key", path)
        if match and method == "PUT":
            if match.group(1) not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            return save_asset_key(match.group(1), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/view", path)
        if match and method == "PUT":
            if match.group(1) not in {item["key"] for item in PROJECTS}:
                return 400, {"message": text["asset-tree.error.project.required"]}
            return save_project_view(match.group(1), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/card", path)
        if match and method in ("GET", "PUT"):
            denied = reject_module(match.group(1), "card", text)
            if denied:
                return denied
            if method == "GET":
                return 200, card_for(match.group(1))
            layout = normalize_card(self.read_json() or {})
            STATE["cards"][match.group(1)] = layout
            return 200, layout
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/flow-graph", path)
        if match and method in ("GET", "PUT"):
            project_key = match.group(1)
            denied = reject_module(project_key, "portal", text)
            if denied:
                return denied
            if method == "GET":
                stored = STATE["flow_graphs"].get(project_key)
                if stored:
                    book = stored if "processes" in stored else normalize_flow_book(stored)
                    return 200, flow_book_named(book)
                graph = flow_graph_from_rules(project_key)
                if graph["nodes"]:
                    return 200, flow_book_named({"processes": [{
                        "id": "rules1", "name": "", "nodes": graph["nodes"], "links": graph["links"],
                    }]})
                return 200, {"processes": []}
            body = self.read_json() or {}
            book = normalize_flow_book(body)
            names = {item["id"]: item["name"] for item in ISSUE_TYPES}
            ensure_statuses(project_key, text)
            known = {row["statusKey"] for row in STATE["statuses"].get(project_key, [])}
            status_names = {item["id"]: item["name"] for item in ISSUE_STATUSES}
            for process in book["processes"]:
                for node in process["nodes"]:
                    if node["kind"] == "request" and node["issueTypeId"] and node["issueTypeId"] not in names:
                        return 400, {"message": text["asset-tree.error.flow.type"]}
                    if node["kind"] == "issue" and node["issueStatusId"] and node["issueStatusId"] not in status_names:
                        return 400, {"message": text.get("asset-tree.error.flow.issue", "Request status")}
                    if node["kind"] == "status" and node["statusKey"]:
                        node["statusKey"] = canonical(node["statusKey"])
                        if node["statusKey"] not in known:
                            return 400, {"message": text["asset-tree.error.flow.status"]}
            STATE["flow_graphs"][project_key] = book
            STATE["flows"][project_key] = flow_rules_from_book(project_key, book, names)
            return 200, flow_book_named(book)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/issue-statuses", path)
        if match and method == "GET":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            return 200, [{"id": item["id"], "name": item["name"]} for item in ISSUE_STATUSES]
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/issue-types", path)
        if match and method == "GET":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            return 200, [{"id": item["id"], "name": item["name"]} for item in ISSUE_TYPES]
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/flows/(\d+)", path)
        if match and method == "DELETE":
            project_key = match.group(1)
            denied = reject_module(project_key, "portal", text)
            if denied:
                return denied
            rule_id = int(match.group(2))
            bucket = STATE["flows"].setdefault(project_key, [])
            kept = [rule for rule in bucket if rule["id"] != rule_id]
            if len(kept) == len(bucket):
                return 404, {"message": text["asset-tree.error.flow.missing"]}
            STATE["flows"][project_key] = kept
            return 204, None
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/flows", path)
        if match and method == "GET":
            project_key = match.group(1)
            denied = reject_module(project_key, "portal", text)
            if denied:
                return denied
            return 200, [dict(rule) for rule in STATE["flows"].get(project_key, [])]
        if match and method == "POST":
            project_key = match.group(1)
            denied = reject_module(project_key, "portal", text)
            if denied:
                return denied
            body = self.read_json() or {}
            type_id = (body.get("issueTypeId") or "").strip()
            found = None
            for item in ISSUE_TYPES:
                if item["id"] == type_id:
                    found = item
            if not found:
                return 400, {"message": text["asset-tree.error.flow.type"]}
            raw_status = (body.get("statusKey") or "").strip()
            if not raw_status:
                return 400, {"message": text["asset-tree.error.flow.status"]}
            ensure_statuses(project_key, text)
            status = canonical(raw_status)
            known = {row["statusKey"] for row in STATE["statuses"].get(project_key, [])}
            if status not in known:
                return 400, {"message": text["asset-tree.error.flow.status"]}
            bucket = STATE["flows"].setdefault(project_key, [])
            for rule in bucket:
                if rule["issueTypeId"] == type_id:
                    rule["statusKey"] = status
                    rule["issueTypeName"] = found["name"]
                    return 201, dict(rule)
            rule = {
                "id": STATE["flow_seq"],
                "issueTypeId": type_id,
                "issueTypeName": found["name"],
                "statusKey": status,
            }
            STATE["flow_seq"] += 1
            bucket.append(rule)
            return 201, dict(rule)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/portal-rules/(\d+)", path)
        if match and method == "PUT":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            ensure_portal_demo()
            rule_id = int(match.group(2))
            found = None
            for rule in STATE["portal_rules"]:
                if rule["id"] == rule_id:
                    found = rule
            if not found:
                return 404, {"message": text["asset-tree.error.portal.missing"]}
            body = self.read_json() or {}
            asset = STATE["assets"].get(int(body.get("assetId") or 0))
            conditions = []
            seen = set()
            for item in body.get("conditions") or []:
                field = " ".join((item.get("field") or "").split())
                option = " ".join((item.get("option") or "").split())
                if not field or not option:
                    continue
                key = field.lower()
                if key in seen:
                    return 400, {"message": text["asset-tree.error.portal.field"]}
                seen.add(key)
                conditions.append({"field": field, "option": option})
            if not asset or asset["projectKey"] != match.group(1):
                return 400, {"message": text["asset-tree.error.portal.target"]}
            if not conditions:
                return 400, {"message": text["asset-tree.error.portal.conditions"]}
            found["assetId"] = asset["id"]
            found["conditions"] = conditions
            return 200, portal_rule_dto(found)
        if match and method == "DELETE":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            ensure_portal_demo()
            rule_id = int(match.group(2))
            before = len(STATE["portal_rules"])
            STATE["portal_rules"] = [rule for rule in STATE["portal_rules"] if rule["id"] != rule_id]
            if len(STATE["portal_rules"]) == before:
                return 404, {"message": text["asset-tree.error.portal.missing"]}
            return 204, None
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/portal-rules", path)
        if match and method == "GET":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            ensure_portal_demo()
            if match.group(1) != "TEST":
                return 200, []
            return 200, [portal_rule_dto(rule) for rule in STATE["portal_rules"]]
        if match and method == "POST":
            denied = reject_module(match.group(1), "portal", text)
            if denied:
                return denied
            ensure_portal_demo()
            body = self.read_json() or {}
            asset = STATE["assets"].get(int(body.get("assetId") or 0))
            conditions = []
            seen = set()
            for item in body.get("conditions") or []:
                field = " ".join((item.get("field") or "").split())
                option = " ".join((item.get("option") or "").split())
                if not field or not option:
                    continue
                key = field.lower()
                if key in seen:
                    return 400, {"message": text["asset-tree.error.portal.field"]}
                seen.add(key)
                conditions.append({"field": field, "option": option})
            if not asset or asset["projectKey"] != match.group(1):
                return 400, {"message": text["asset-tree.error.portal.target"]}
            if not conditions:
                return 400, {"message": text["asset-tree.error.portal.conditions"]}
            rule_id = STATE["portal_seq"]
            STATE["portal_seq"] += 1
            rule = {"id": rule_id, "assetId": asset["id"], "position": rule_id, "conditions": conditions}
            STATE["portal_rules"].append(rule)
            return 201, portal_rule_dto(rule)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/picker", path)
        if match and method == "GET":
            return 200, picker_nodes(project_assets(match.group(1)), text)
        if path == "/types" and method == "POST":
            return self.create_type(self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/types/arrange", path)
        if match and method == "POST":
            return self.arrange_type(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/types", path)
        if match and method == "POST":
            return self.offer_type(int(match.group(1)), self.read_json(), True, text)
        match = re.fullmatch(r"/assets/(\d+)/types/([^/]+)", path)
        if match and method == "DELETE":
            return self.offer_type(int(match.group(1)), {"typeKey": match.group(2)}, False, text)
        match = re.fullmatch(r"/assets/(\d+)/plans", path)
        if match and method == "POST":
            return self.create_plan(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/plans/(\d+)", path)
        if match and method == "PUT":
            return self.update_plan(int(match.group(1)), int(match.group(2)), self.read_json(), text)
        if match and method == "DELETE":
            return self.delete_plan(int(match.group(1)), int(match.group(2)), text)
        match = re.fullmatch(r"/assets/(\d+)/copy", path)
        if match and method == "POST":
            return self.copy_asset(int(match.group(1)), text)
        match = re.fullmatch(r"/assets/(\d+)", path)
        if match and method == "GET":
            asset = STATE["assets"].get(int(match.group(1)))
            if not asset:
                return 404, {"message": text["asset-tree.error.notFound"]}
            apply_due(asset["projectKey"])
            asset = STATE["assets"].get(int(match.group(1)))
            return 200, asset_dto(asset, text, True)
        if match and method == "PUT":
            return self.update_asset(int(match.group(1)), self.read_json(), text)
        if match and method == "DELETE":
            return self.delete_asset(int(match.group(1)), (query.get("cascade") or ["false"])[0] == "true", text)
        match = re.fullmatch(r"/assets/(\d+)/comments", path)
        if match and method == "POST":
            return self.add_comment(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/comments/(\d+)", path)
        if match and method == "DELETE":
            return self.delete_comment(int(match.group(1)), int(match.group(2)), text)
        match = re.fullmatch(r"/assets/(\d+)/activities/(\d+)", path)
        if match and method == "DELETE":
            row = STATE["activities"].get(int(match.group(2)))
            if not row or row["assetId"] != int(match.group(1)):
                return 404, {"message": text["asset-tree.error.notFound"]}
            STATE["activities"].pop(int(match.group(2)), None)
            return 204, None
        match = re.fullmatch(r"/assets/(\d+)/files/(\d+)", path)
        if match and method == "DELETE":
            return self.delete_file(int(match.group(1)), int(match.group(2)), text)
        match = re.fullmatch(r"/assets/(\d+)/move", path)
        if match and method == "POST":
            return self.move_asset(int(match.group(1)), self.read_json(), text)
        match = re.fullmatch(r"/assets/(\d+)/issues", path)
        if match and method == "POST":
            return self.link_issue(int(match.group(1)), (self.read_json() or {}).get("issueKey"), text)
        match = re.fullmatch(r"/assets/(\d+)/issues/(\d+)", path)
        if match and method == "DELETE":
            had = next((link for link in STATE["links"] if link["assetId"] == int(match.group(1)) and link["issueId"] == int(match.group(2))), None)
            STATE["links"] = [link for link in STATE["links"] if not (link["assetId"] == int(match.group(1)) and link["issueId"] == int(match.group(2)))]
            if had:
                apply_removed_flow(int(match.group(1)), ISSUES_BY_ID.get(int(match.group(2))), had.get("statusBefore") or "")
            return 204, None
        match = re.fullmatch(r"/issues/(\d+)/context", path)
        if match and method == "GET":
            issue = ISSUES_BY_ID.get(int(match.group(1)))
            if not issue:
                return 404, {"message": text["asset-tree.error.issue.notFound"]}
            project = next((item for item in PROJECTS if item["key"] == issue["projectKey"]), None)
            if not project:
                return 404, {"message": text["asset-tree.error.project.notFound"]}
            return 200, {"projectKey": project["key"], "projectName": project["name"], "canEdit": True}
        match = re.fullmatch(r"/issues/(\d+)/search", path)
        if match and method == "GET":
            issue = ISSUES_BY_ID.get(int(match.group(1)))
            if not issue:
                return 404, {"message": text["asset-tree.error.issue.notFound"]}
            needle = (query.get("q") or [""])[0].strip().lower()
            rows = [row for row in project_assets(issue["projectKey"]) if needle in (row["name"] + row["objectKey"]).lower()]
            return 200, [asset_dto(row, text, False) for row in rows[:8]]
        match = re.fullmatch(r"/issues/(\d+)/assets", path)
        if match and method == "GET":
            issue_id = int(match.group(1))
            if issue_id not in ISSUES_BY_ID:
                return 404, {"message": text["asset-tree.error.issue.notFound"]}
            linked = [STATE["assets"][link["assetId"]] for link in STATE["links"] if link["issueId"] == issue_id and link["assetId"] in STATE["assets"]]
            return 200, [asset_dto(asset, text, False) for asset in linked]
        if match and method == "POST":
            body = self.read_json() or {}
            issue = ISSUES_BY_ID.get(int(match.group(1)))
            asset = STATE["assets"].get(int(body.get("assetId") or 0))
            if not issue or not asset:
                return 404, {"message": text["asset-tree.error.notFound"]}
            return self.link_issue(asset["id"], issue["issueKey"], text)
        match = re.fullmatch(r"/types/([a-z0-9-]+)/fields", path)
        if match and method == "POST":
            return self.add_field(match.group(1), self.read_json(), text)
        match = re.fullmatch(r"/types/([a-z0-9-]+)/field-order", path)
        if match and method == "PUT":
            row = STATE["types"].get(match.group(1))
            if not row:
                return 404, {"message": text["asset-tree.error.type.notFound"]}
            wanted = (self.read_json() or {}).get("fieldKeys") or []
            by_key = {field["fieldKey"]: field for field in row["fields"]}
            ordered = [by_key.pop(key) for key in wanted if key in by_key]
            ordered += [field for field in row["fields"] if field["fieldKey"] in by_key]
            for index, field in enumerate(ordered):
                field["position"] = index
            row["fields"] = ordered
            return 200, [{
                "fieldKey": field["fieldKey"], "label": label_of(field["label"], text), "kind": field["kind"],
                "required": field["required"], "position": field["position"],
                "showOnIssue": field["showOnIssue"] if "showOnIssue" in field else True,
                "options": field.get("options") or [],
            } for field in ordered]
        match = re.fullmatch(r"/types/([a-z0-9-]+)/fields/([a-z0-9-]+)", path)
        if match and method == "PUT":
            row = STATE["types"].get(match.group(1))
            if not row:
                return 404, {"message": text["asset-tree.error.type.notFound"]}
            body = self.read_json() or {}
            for field in row["fields"]:
                if field["fieldKey"] != match.group(2):
                    continue
                if "label" in body:
                    label = str(body.get("label") or "").strip()
                    if not label or len(label) > 80:
                        return 400, {"message": text.get("asset-tree.error.field.label", "Label")}
                    field["label"] = label
                if "kind" in body and body.get("kind"):
                    kind = str(body.get("kind"))
                    if kind not in FIELD_KINDS:
                        return 400, {"message": text.get("asset-tree.error.field.kind", "Kind")}
                    options = field.get("options") or []
                    if kind in OPTION_KINDS:
                        raw = body.get("options")
                        if raw is None:
                            raw = "\n".join(options) if isinstance(options, list) else options
                        options = canonical_options(raw)
                        if options is None:
                            return 400, {"message": text.get("asset-tree.error.field.options", "Choices")}
                    else:
                        options = []
                    field["kind"] = kind
                    field["options"] = options
                elif "options" in body and field.get("kind") in OPTION_KINDS:
                    options = canonical_options(body.get("options"))
                    if options is None:
                        return 400, {"message": text.get("asset-tree.error.field.options", "Choices")}
                    field["options"] = options
                if "required" in body:
                    field["required"] = bool(body.get("required"))
                if "showOnIssue" in body:
                    field["showOnIssue"] = bool(body.get("showOnIssue"))
                return 200, {
                    "fieldKey": field["fieldKey"], "label": label_of(field["label"], text), "kind": field["kind"],
                    "required": field["required"], "position": field["position"],
                    "showOnIssue": field["showOnIssue"] if "showOnIssue" in field else True,
                    "options": field.get("options") or [],
                }
            return 404, {"message": text.get("asset-tree.error.field.notFound", "Field")}
        if match and method == "DELETE":
            row = STATE["types"].get(match.group(1))
            if not row:
                return 404, {"message": text["asset-tree.error.type.notFound"]}
            row["fields"] = [field for field in row["fields"] if field["fieldKey"] != match.group(2)]
            return 204, None
        match = re.fullmatch(r"/types/([a-z0-9-]+)", path)
        if match and method == "PUT":
            return self.update_type(match.group(1), self.read_json(), text)
        if match and method == "DELETE":
            row = STATE["types"].get(match.group(1))
            if not row:
                return 404, {"message": text["asset-tree.error.type.notFound"]}
            if row["systemType"]:
                return 400, {"message": text["asset-tree.error.type.system"]}
            if any(asset["typeKey"] == row["typeKey"] for asset in STATE["assets"].values()):
                return 409, {"message": text["asset-tree.error.type.inUse"]}
            if child_types(row["typeKey"]):
                return 409, {"message": text.get("asset-tree.error.type.hasSubtypes", "Subtypes")}
            del STATE["types"][row["typeKey"]]
            return 204, None
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/statuses", path)
        if match and method == "GET":
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 404, {"message": text["asset-tree.error.project.notFound"]}
            return 200, status_dtos(project_key, text)
        if match and method == "POST":
            return self.create_status(match.group(1), self.read_json(), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/statuses/([a-z0-9_-]+)", path)
        if match and method == "PUT":
            return self.update_status(match.group(1), match.group(2), self.read_json(), text)
        if match and method == "DELETE":
            return self.delete_status(match.group(1), match.group(2), text)
        match = re.fullmatch(r"/projects/([A-Za-z0-9]+)/grants", path)
        if match and method == "GET":
            project_key = match.group(1)
            if project_key not in {item["key"] for item in PROJECTS}:
                return 404, {"message": text["asset-tree.error.project.notFound"]}
            return 200, list(STATE["grants"].get(project_key, []))
        if match and method == "POST":
            return self.add_grant(match.group(1), self.read_json(), text)
        if match and method == "DELETE":
            return self.delete_grant(match.group(1), (query.get("group") or [""])[0], text)
        if path == "/groups" and method == "GET":
            needle = (query.get("q") or [""])[0].strip().lower()
            found = [{"groupName": name} for name in GROUPS if needle and needle in name.lower()]
            return 200, found[:15]
        return 404, {"message": text["asset-tree.error.notFound"]}

    def create_status(self, project_key, body, text):
        if project_key not in {item["key"] for item in PROJECTS}:
            return 404, {"message": text["asset-tree.error.project.notFound"]}
        label = ((body or {}).get("label") or "").strip()
        if not label:
            return 400, {"message": text["asset-tree.error.status"]}
        category = ((body or {}).get("category") or "todo").strip() or "todo"
        if category not in CATEGORY_COLOR:
            return 400, {"message": text["asset-tree.error.status"]}
        rows = ensure_statuses(project_key, text)
        taken = {row["statusKey"] for row in rows}
        key = unique_key(slug(label), taken)
        if not STATUS_KEY.match(key):
            return 400, {"message": text["asset-tree.error.status"]}
        raw_summary = (body or {}).get("inSummary", None)
        if "inSummary" not in (body or {}) or raw_summary is None:
            mode = 1
        else:
            mode = 1 if raw_summary else 2
        row = {
            "statusKey": key, "label": label, "category": category,
            "sortOrder": len(rows), "summaryMode": mode,
        }
        rows.append(row)
        return 201, status_payload(row, 0)

    def update_status(self, project_key, status_key, body, text):
        rows = ensure_statuses(project_key, text)
        row = next((item for item in rows if item["statusKey"] == status_key), None)
        if not row:
            return 404, {"message": text["asset-tree.error.status"]}
        label = ((body or {}).get("label") or "").strip()
        if (body or {}).get("label") is not None and not label:
            return 400, {"message": text["asset-tree.error.status"]}
        if label:
            row["label"] = label
        category = ((body or {}).get("category") or "").strip()
        if category:
            if category not in CATEGORY_COLOR:
                return 400, {"message": text["asset-tree.error.status"]}
            row["category"] = category
        if "inSummary" in (body or {}) and (body or {}).get("inSummary") is not None:
            row["summaryMode"] = 1 if body.get("inSummary") else 2
        count = len([
            asset for asset in project_assets(project_key)
            if canonical(asset.get("status")) == status_key and not is_location_id(asset["id"])
        ])
        return 200, status_payload(row, count)

    def delete_status(self, project_key, status_key, text):
        rows = ensure_statuses(project_key, text)
        if len(rows) <= 1:
            return 400, {"message": text["asset-tree.error.status.last"]}
        row = next((item for item in rows if item["statusKey"] == status_key), None)
        if not row:
            return 404, {"message": text["asset-tree.error.status"]}
        used = any(
            canonical(asset.get("status")) == status_key and not is_location_id(asset["id"])
            for asset in project_assets(project_key)
        )
        if used:
            return 409, {"message": text["asset-tree.error.status.inUse"]}
        fallback = next((item["statusKey"] for item in rows if item["statusKey"] != status_key), None)
        if fallback:
            for asset in project_assets(project_key):
                if is_location_id(asset["id"]) and canonical(asset.get("status")) == status_key:
                    asset["status"] = fallback
        STATE["statuses"][project_key] = [item for item in rows if item["statusKey"] != status_key]
        return 204, None

    def add_grant(self, project_key, body, text):
        if project_key not in {item["key"] for item in PROJECTS}:
            return 404, {"message": text["asset-tree.error.project.notFound"]}
        group_name = ((body or {}).get("groupName") or "").strip()
        raw_caps = (body or {}).get("caps")
        level = ((body or {}).get("level") or "").strip()
        caps = normalize_caps(raw_caps) if raw_caps else caps_from_level(level)
        if group_name not in GROUPS or not caps:
            return 400, {"message": text["asset-tree.error.group"]}
        row = {"groupName": group_name, "level": level_of(caps), "caps": caps}
        rows = STATE["grants"].setdefault(project_key, [])
        current = next((item for item in rows if item["groupName"] == group_name), None)
        if current:
            current.update(row)
            return 201, current
        rows.append(row)
        rows.sort(key=lambda item: item["groupName"].lower())
        return 201, row

    def delete_grant(self, project_key, group_name, text):
        rows = STATE["grants"].get(project_key, [])
        match = next((item for item in rows if item["groupName"] == (group_name or "").strip()), None)
        if not match:
            return 404, {"message": text["asset-tree.error.group"]}
        STATE["grants"][project_key] = [item for item in rows if item is not match]
        return 204, None

    def project_types(self, project_key):
        rows = [row for row in STATE["types"].values() if row["projectKey"] == project_key]
        rows.sort(key=lambda item: item["sortOrder"])
        return rows

    def values_from(self, body, type_key, text):
        row = type_row(type_key)
        if not row:
            return text["asset-tree.error.type"], {}
        incoming = {}
        for item in (body or {}).get("attributes") or []:
            if item and item.get("fieldKey"):
                incoming[item["fieldKey"]] = item.get("value") or ""
        values = {}
        for field in row["fields"]:
            value = (incoming.get(field["fieldKey"]) or "").strip()
            if field["required"] and not value:
                return text["asset-tree.error.field.required"].replace("{0}", label_of(field["label"], text)), {}
            stored = incoming.get(field["fieldKey"]) or ""
            if field["kind"] in OPTION_KINDS | {"labels", "url", "version"} and value:
                stored = canonical_field_value(field["kind"], field.get("options") or [], value)
                if stored is None:
                    if field["kind"] == "url":
                        return text.get("asset-tree.error.url", "Link"), {}
                    if field["kind"] == "version":
                        return text.get("asset-tree.error.version", "Version"), {}
                    if field["kind"] == "labels":
                        return text.get("asset-tree.error.label", "Label"), {}
                    return text.get("asset-tree.error.field.choice", "Choice"), {}
            if field["kind"] == "date" and value:
                stored = canonical_date(value)
                if not stored:
                    return text.get("asset-tree.error.date", "Date"), {}
            if field["kind"] == "user" and value and value not in USERS:
                return text["asset-tree.error.user"], {}
            values[field["fieldKey"]] = stored
        return None, values

    def offer_type(self, asset_id, body, present, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        if not is_location_id(asset_id):
            return 400, {"message": text.get("asset-tree.error.type.place", "Place")}
        raw_keys = (body or {}).get("typeKeys") or []
        if isinstance(raw_keys, str):
            raw_keys = [raw_keys]
        type_keys = []
        single = ((body or {}).get("typeKey") or "").strip()
        for key in list(raw_keys) + ([single] if single else []):
            key = str(key or "").strip()
            if key and key not in type_keys:
                type_keys.append(key)
        if not type_keys or len(type_keys) > 40:
            return 400, {"message": text["asset-tree.error.type"]}
        for type_key in type_keys:
            row = STATE["types"].get(type_key)
            if not row or row.get("location") or row.get("projectKey") != asset["projectKey"]:
                return 400, {"message": text["asset-tree.error.type"]}
        offered = list(asset.get("offeredTypes") or [])
        facts = place_facts(asset["projectKey"])
        if present:
            for type_key in type_keys:
                nxt = add_pin(offered, facts, type_key)
                if nxt is None:
                    return 400, {"message": text["asset-tree.error.type"]}
                offered = nxt
        else:
            for type_key in type_keys:
                direct = any(
                    item.get("parentId") == asset_id and item.get("typeKey") == type_key and not is_location_id(item["id"])
                    for item in STATE["assets"].values()
                )
                if direct:
                    return 409, {"message": text.get("asset-tree.error.type.busy", "Busy")}
            for type_key in type_keys:
                offered = remove_pin(offered, type_key)
        asset["offeredTypes"] = offered
        return (200, asset_dto(asset, text, False)) if present else (204, None)

    def arrange_type(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        if not is_location_id(asset_id):
            return 400, {"message": text.get("asset-tree.error.type.place", "Place")}
        type_key = ((body or {}).get("typeKey") or "").strip()
        row = STATE["types"].get(type_key)
        if not row or row.get("location") or row.get("projectKey") != asset["projectKey"]:
            return 400, {"message": text.get("asset-tree.error.type.arrange", "Move")}
        pinned = []
        for line in asset.get("offeredTypes") or []:
            key = line_key(line)
            if key and key not in pinned:
                pinned.append(key)
        for item in STATE["assets"].values():
            if item.get("parentId") == asset_id and not is_location_id(item["id"]):
                key = item.get("typeKey") or ""
                if key and key not in pinned:
                    pinned.append(key)
        nxt = place_move(list(asset.get("offeredTypes") or []), pinned, place_facts(asset["projectKey"]), type_key, (body or {}).get("action"))
        if nxt is None:
            return 400, {"message": text.get("asset-tree.error.type.arrange", "Move")}
        asset["offeredTypes"] = nxt
        return 200, asset_dto(asset, text, False)

    def create_plan(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        if is_location_id(asset_id):
            return 400, {"message": text.get("asset-tree.error.service.place", "Equipment")}
        if not module_on(asset["projectKey"], "service"):
            return 400, {"message": text.get("asset-tree.error.module.off", "Module")}
        if not (STATE["types"].get(asset["typeKey"]) or {}).get("service"):
            return 400, {"message": text.get("asset-tree.error.service.type", "Type")}
        if len(plan_rows(asset_id)) >= 12:
            return 400, {"message": text.get("asset-tree.error.service.limit", "Limit")}
        plan_id = STATE["plan_seq"]
        STATE["plan_seq"] += 1
        row = {"id": plan_id, "assetId": asset_id, "appliedFor": "", "notify": True}
        error = fill_plan(row, body, True, asset["projectKey"], text)
        if error:
            return 400, {"message": error}
        STATE["plans"][plan_id] = row
        apply_due(asset["projectKey"])
        return 201, plan_dto(STATE["plans"][plan_id])

    def update_plan(self, asset_id, plan_id, body, text):
        asset = STATE["assets"].get(asset_id)
        row = STATE["plans"].get(plan_id)
        if not asset or not row or row["assetId"] != asset_id:
            return 404, {"message": text["asset-tree.error.notFound"]}
        error = fill_plan(row, body, False, asset["projectKey"], text)
        if error:
            return 400, {"message": error}
        apply_due(asset["projectKey"])
        return 200, plan_dto(row)

    def delete_plan(self, asset_id, plan_id, text):
        row = STATE["plans"].get(plan_id)
        if not STATE["assets"].get(asset_id) or not row or row["assetId"] != asset_id:
            return 404, {"message": text["asset-tree.error.notFound"]}
        STATE["plans"].pop(plan_id, None)
        return 204, None

    def create_asset(self, body, text):
        if not body or not (body.get("name") or "").strip():
            return 400, {"message": text["asset-tree.error.name.required"]}
        project_key = body.get("projectKey")
        if project_key not in {item["key"] for item in PROJECTS}:
            return 400, {"message": text["asset-tree.error.project.required"]}
        if not asset_prefix(project_key):
            return 400, {"message": text.get("asset-tree.error.assetKey.required", "Key")}
        type_key = (body.get("typeKey") or "").strip()
        row = type_row(type_key)
        if not row or row["projectKey"] != project_key:
            return 400, {"message": text["asset-tree.error.type"]}
        parent_id = normalize_parent(body.get("parentId"))
        parent = STATE["assets"].get(parent_id) if parent_id else None
        if parent_id and (not parent or parent["projectKey"] != project_key):
            return 400, {"message": text["asset-tree.error.project.mismatch"]}
        custodian = (body.get("custodianKey") or "").strip() or None
        if custodian and custodian not in USERS:
            return 400, {"message": text["asset-tree.error.user"]}
        error, values = self.values_from(body, type_key, text)
        if error:
            return 400, {"message": error}
        status = canonical(body.get("status") or "in_use")
        if not status_known(project_key, status, text):
            return 400, {"message": text["asset-tree.error.status"]}
        asset_id = add_asset(project_key, type_key, body["name"].strip(), parent_id, status, custodian, values)
        if not asset_id:
            return 400, {"message": text.get("asset-tree.error.assetKey.required", "Key")}
        STATE["assets"][asset_id]["description"] = body.get("description") or ""
        log_activity(asset_id, "created", "", "", body["name"].strip())
        log_place(place_container(parent_id), "place_add", STATE["assets"][asset_id])
        return 201, asset_dto(STATE["assets"][asset_id], text, True)

    def copy_asset(self, asset_id, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        row = type_row(asset["typeKey"])
        if not row or row.get("location"):
            return 400, {"message": text.get("asset-tree.error.copy", "Only equipment can be copied.")}
        if not asset_prefix(asset["projectKey"]):
            return 400, {"message": text.get("asset-tree.error.assetKey.required", "Key")}
        taken = {item["name"] for item in STATE["assets"].values() if item["projectKey"] == asset["projectKey"]}
        name = copy_name(asset["name"], text.get("asset-tree.ui.copyWord", "copy"), taken)
        parent_id = normalize_parent(asset.get("parentId"))
        new_id = add_asset(
            asset["projectKey"], asset["typeKey"], name, parent_id,
            canonical(asset.get("status") or "in_use"), asset.get("custodianKey"),
            dict(asset.get("values") or {}),
        )
        if not new_id:
            return 400, {"message": text.get("asset-tree.error.assetKey.required", "Key")}
        STATE["assets"][new_id]["description"] = asset.get("description") or ""
        log_activity(new_id, "created", "", "", name)
        log_place(place_container(parent_id), "place_add", STATE["assets"][new_id])
        return 201, asset_dto(STATE["assets"][new_id], text, True)

    def update_asset(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        if not body or not (body.get("name") or "").strip():
            return 400, {"message": text["asset-tree.error.name.required"]}
        type_key = (body.get("typeKey") or asset["typeKey"]).strip()
        row = type_row(type_key)
        if not row or row["projectKey"] != asset["projectKey"]:
            return 400, {"message": text["asset-tree.error.type"]}
        status = canonical(body.get("status") or asset["status"])
        if not status_known(asset["projectKey"], status, text):
            return 400, {"message": text["asset-tree.error.status"]}
        custodian = (body.get("custodianKey") or "").strip() or None
        if custodian and custodian not in USERS:
            return 400, {"message": text["asset-tree.error.user"]}
        error, values = self.values_from(body, type_key, text)
        if error:
            return 400, {"message": error}
        old_name = asset["name"]
        old_description = asset.get("description") or ""
        old_type = asset["typeKey"]
        old_status = canonical(asset["status"])
        old_custodian = asset.get("custodianKey") or ""
        old_values = dict(asset.get("values") or {})
        new_name = body["name"].strip()
        new_description = body.get("description") or ""
        new_status = canonical(status)
        new_custodian = custodian or ""
        asset.update({"name": new_name, "description": new_description, "typeKey": type_key, "status": new_status, "custodianKey": custodian, "values": values, "updated": now_stamp()})
        log_activity(asset_id, "name", "", old_name, new_name)
        log_activity(asset_id, "description", "", old_description, new_description)
        old_type_row = type_row(old_type)
        new_type_row = type_row(type_key)
        log_activity(asset_id, "type", "", (old_type_row or {}).get("label") or old_type, (new_type_row or {}).get("label") or type_key)
        log_activity(asset_id, "status", "", old_status, new_status)
        old_person = USERS.get(old_custodian)
        new_person = USERS.get(new_custodian)
        log_activity(asset_id, "custodian", "", old_person["displayName"] if old_person else "", new_person["displayName"] if new_person else "")
        labels = {}
        for field in (new_type_row or {}).get("fields") or []:
            labels[field["fieldKey"]] = label_of(field["label"], text)
        keys = set(old_values) | set(values)
        for key in keys:
            old_value = (old_values.get(key) or "").strip()
            new_value = (values.get(key) or "").strip()
            log_activity(asset_id, "attribute", labels.get(key) or key, old_value, new_value)
        return 200, asset_dto(asset, text, True)

    def delete_asset(self, asset_id, cascade, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        rows = project_assets(asset["projectKey"])
        nested = descendants(rows, asset_id)
        if nested and not cascade:
            return 409, {"message": text["asset-tree.error.hasChildren"].replace("{0}", str(len(nested)))}
        doomed = set(nested + [asset_id])
        for current in nested + [asset_id]:
            gone = STATE["assets"].get(current)
            if gone:
                log_place(place_container(gone.get("parentId"), doomed), "place_remove", gone)
        for current in nested + [asset_id]:
            STATE["assets"].pop(current, None)
            STATE["checks"].pop(current, None)
            STATE["links"] = [link for link in STATE["links"] if link["assetId"] != current]
            for comment_id in [row["id"] for row in comments_of(current)]:
                STATE["comments"].pop(comment_id, None)
            for file_id in [row["id"] for row in files_of(current)]:
                STATE["files"].pop(file_id, None)
                delete_preview_file(file_id)
            STATE["card_images"].pop(current, None)
            delete_card_image(current)
            for activity_id in [row["id"] for row in activities_of(current)]:
                STATE["activities"].pop(activity_id, None)
            for plan_id in [row["id"] for row in plan_rows(current)]:
                STATE["plans"].pop(plan_id, None)
            STATE.setdefault("lives", {}).pop(current, None)
            for floor in STATE.setdefault("floors", {}).values():
                floor["pins"] = [pin for pin in floor.get("pins") or [] if pin.get("assetId") != current]
        return 204, None

    def move_asset(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        parent_id = normalize_parent((body or {}).get("parentId"))
        parent = STATE["assets"].get(parent_id) if parent_id else None
        if parent_id and (not parent or parent["projectKey"] != asset["projectKey"]):
            return 400, {"message": text["asset-tree.error.project.mismatch"]}
        rows = project_assets(asset["projectKey"])
        if would_cycle(rows, asset_id, parent_id):
            return 400, {"message": text["asset-tree.error.cycle"]}
        base = 0 if parent_id is None else depth(rows, parent_id) + 1
        if base >= MAX_DEPTH or base + subtree_height(rows, asset_id) >= MAX_DEPTH:
            return 400, {"message": text["asset-tree.error.depth"]}
        siblings = [row for row in rows if row["id"] != asset_id and normalize_parent(row["parentId"]) == parent_id]
        siblings.sort(key=lambda item: (item["sortOrder"], item["id"]))
        index = None if body is None else body.get("index")
        if index is None:
            index = len(siblings)
        index = max(0, min(int(index), len(siblings)))
        previous_parent = normalize_parent(asset.get("parentId"))
        from_place = place_container(previous_parent)
        to_place = place_container(parent_id)
        asset["parentId"] = parent_id
        if previous_parent != parent_id:
            old_name = STATE["assets"].get(previous_parent, {}).get("name", "") if previous_parent else ""
            new_name = parent["name"] if parent else ""
            log_activity(asset_id, "move", "", old_name, new_name)
        if from_place and from_place != to_place:
            log_place(from_place, "place_out", asset)
        if to_place and to_place != from_place:
            log_place(to_place, "place_in", asset)
        siblings.insert(index, asset)
        for position, sibling in enumerate(siblings):
            sibling["sortOrder"] = position
        return 200, asset_dto(asset, text, True)

    def link_issue(self, asset_id, issue_key, text):
        asset = STATE["assets"].get(asset_id)
        issue = ISSUES.get((issue_key or "").strip().upper())
        if not asset or not issue:
            return 404, {"message": text["asset-tree.error.issue.notFound"] if not issue else text["asset-tree.error.notFound"]}
        if asset["projectKey"] != issue["projectKey"]:
            return 400, {"message": text["asset-tree.error.project.mismatch"]}
        if any(link["assetId"] == asset_id and link["issueId"] == issue["issueId"] for link in STATE["links"]):
            return 409, {"message": text["asset-tree.error.issue.duplicate"]}
        STATE["links"].append({"assetId": asset_id, "issueId": issue["issueId"], "statusBefore": STATE["assets"][asset_id].get("status") or ""})
        return 201, issue

    def create_type(self, body, text):
        label = ((body or {}).get("label") or "").strip()
        project_key = (body or {}).get("projectKey")
        if not label:
            return 400, {"message": text["asset-tree.error.type.label"]}
        if project_key not in {item["key"] for item in PROJECTS}:
            return 400, {"message": text["asset-tree.error.project.required"]}
        color = (body or {}).get("color") or PALETTE[0]
        location = bool((body or {}).get("location"))
        icon = resolve_icon((body or {}).get("icon"), location)
        if icon is None:
            return 400, {"message": text["asset-tree.error.type.icon"]}
        caption = ""
        if location:
            caption = str((body or {}).get("placeCaption") or "").strip()
            if len(caption) > 80:
                return 400, {"message": text.get("asset-tree.error.caption.length", "Caption")}
        taken = set(STATE["types"])
        key = unique_key(slug(label) or "type", taken)
        parent = resolve_parent(project_key, None, (body or {}).get("parentTypeKey"), location)
        if parent is None:
            return 400, {"message": text.get("asset-tree.error.type.parent", "Parent")}
        order = max([item["sortOrder"] for item in self.project_types(project_key)] or [-1]) + 1
        STATE["types"][key] = {
            "typeKey": key, "projectKey": project_key, "baseKey": "", "label": label, "color": color, "icon": icon,
            "systemType": False, "location": location, "placeCaption": caption,
            "showInTree": location or bool((body or {}).get("showInTree")),
            "service": (not location) and bool((body or {}).get("service")),
            "parentTypeKey": parent,
            "sortOrder": order, "fields": [],
        }
        return 201, type_dto(STATE["types"][key], text, project_assets(project_key))

    def update_type(self, type_key, body, text):
        row = STATE["types"].get(type_key)
        if not row:
            return 404, {"message": text["asset-tree.error.type.notFound"]}
        body = body or {}
        if body.get("showInTree") is not None:
            row["showInTree"] = bool(row.get("location")) or bool(body.get("showInTree"))
        if body.get("service") is not None and not row.get("location"):
            row["service"] = bool(body.get("service"))
        if body.get("icon") is not None:
            icon = resolve_icon(body.get("icon"), bool(row.get("location")))
            if icon is None:
                return 400, {"message": text["asset-tree.error.type.icon"]}
            row["icon"] = icon
        if body.get("color") is not None:
            color = str(body.get("color")).strip()
            if not re.fullmatch(r"#[0-9A-Fa-f]{6}", color):
                return 400, {"message": text["asset-tree.error.type.color"]}
            row["color"] = color
        if body.get("placeCaption") is not None and row.get("location"):
            caption = str(body.get("placeCaption") or "").strip()
            if len(caption) > 80:
                return 400, {"message": text.get("asset-tree.error.caption.length", "Caption")}
            row["placeCaption"] = caption
        if "parentTypeKey" in body:
            parent = resolve_parent(row["projectKey"], type_key, body.get("parentTypeKey"), bool(row.get("location")))
            if parent is None:
                return 400, {"message": text.get("asset-tree.error.type.parent", "Parent")}
            row["parentTypeKey"] = parent
        return 200, type_dto(row, text, project_assets(row["projectKey"]))

    def add_field(self, type_key, body, text):
        row = STATE["types"].get(type_key)
        label = ((body or {}).get("label") or "").strip()
        kind = (body or {}).get("kind") or "text"
        if not row:
            return 404, {"message": text["asset-tree.error.type.notFound"]}
        if not label:
            return 400, {"message": text["asset-tree.error.field.label"]}
        if kind not in FIELD_KINDS:
            return 400, {"message": text["asset-tree.error.field.kind"]}
        options = []
        if kind in OPTION_KINDS:
            options = canonical_options((body or {}).get("options"))
            if options is None:
                return 400, {"message": text.get("asset-tree.error.field.options", "Choices")}
        taken = {field["fieldKey"] for field in row["fields"]}
        field_key = unique_key(slug(label) or "field", taken)
        shown = True if (body or {}).get("showOnIssue") is None else bool(body.get("showOnIssue"))
        field = {
            "fieldKey": field_key, "label": label, "kind": kind, "required": bool((body or {}).get("required")),
            "showOnIssue": shown,
            "position": len(row["fields"]), "options": options,
        }
        row["fields"].append(field)
        return 201, {
            "fieldKey": field_key, "label": label, "kind": kind, "required": field["required"],
            "showOnIssue": shown,
            "position": field["position"], "options": options,
        }

    def ancestors_of(self, asset):
        chain = []
        cursor = normalize_parent(asset.get("parentId"))
        guard = 0
        while cursor and guard < 40:
            chain.append(cursor)
            parent = STATE["assets"].get(cursor)
            cursor = normalize_parent(parent.get("parentId")) if parent else None
            guard += 1
        chain.reverse()
        return chain

    def inventory(self, project_key, text):
        rows = project_assets(project_key)
        types = {row["typeKey"]: row for row in self.project_types(project_key)}
        items = []
        checked = 0
        for asset in rows:
            kind = types.get(asset["typeKey"])
            if kind and kind.get("location"):
                continue
            mark = STATE["checks"].get(asset["id"])
            if mark:
                checked += 1
            person = USERS.get(asset.get("custodianKey") or "")
            items.append({
                "id": asset["id"],
                "name": asset["name"],
                "objectKey": asset["objectKey"],
                "typeLabel": (kind or {}).get("label") or asset["typeKey"],
                "status": canonical(asset["status"]),
                "location": location_of(asset),
                "ancestors": self.ancestors_of(asset),
                "custodian": person["displayName"] if person else "",
                "checked": bool(mark),
                "checkedAt": mark["checkedAt"] if mark else "",
                "checkedBy": mark["checkedBy"] if mark else "",
            })
        items.sort(key=lambda item: (item["location"].lower(), item["name"].lower()))
        return {"total": len(items), "checked": checked, "rows": items}

    def mark_inventory(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        if (body or {}).get("checked"):
            STATE["checks"][asset_id] = {"checkedAt": "2026-09-27T10:00:00Z", "checkedBy": "Preview"}
        else:
            STATE["checks"].pop(asset_id, None)
        for row in self.inventory(asset["projectKey"], text)["rows"]:
            if row["id"] == asset_id:
                return 200, row
        return 200, {"id": asset_id, "checked": bool(STATE["checks"].get(asset_id)), "ancestors": []}

    def report(self, project_key, text):
        rows = project_assets(project_key)
        types = self.project_types(project_key)
        location_keys = {row["typeKey"] for row in types if row["location"]}
        equipment = [row for row in rows if row["typeKey"] not in location_keys]
        by_status = []
        for status in status_dtos(project_key, text):
            count = len([row for row in equipment if canonical(row["status"]) == status["statusKey"]])
            if count:
                by_status.append({
                    "key": status["statusKey"],
                    "label": status["label"],
                    "color": CATEGORY_COLOR.get(status["category"], "#4a6785"),
                    "count": count,
                })
        by_type = []
        for row in types:
            if row["typeKey"] in location_keys:
                continue
            count = len([asset for asset in equipment if asset["typeKey"] == row["typeKey"]])
            if count:
                typed = type_dto(row, text, rows)
                by_type.append({"key": row["typeKey"], "label": typed["label"], "color": row["color"], "icon": typed["icon"], "count": count})
        places = []
        for asset in rows:
            if asset["typeKey"] not in location_keys:
                continue
            nested = [item for item in descendants(rows, asset["id"]) if STATE["assets"][item]["typeKey"] not in location_keys]
            places.append({"id": asset["id"], "name": asset["name"], "typeLabel": type_dto(type_row(asset["typeKey"]), text, rows)["label"], "equipment": len(nested)})
        holders = {}
        for asset in equipment:
            if asset.get("custodianKey"):
                holders[asset["custodianKey"]] = holders.get(asset["custodianKey"], 0) + 1
        project = next(item for item in PROJECTS if item["key"] == project_key)
        return {
            "projectKey": project_key, "projectName": project["name"], "total": len(rows), "equipment": len(equipment),
            "unassigned": len([row for row in equipment if not row.get("custodianKey")]),
            "byStatus": by_status, "byType": by_type, "places": places,
            "holders": [{"user": USERS[key], "count": count} for key, count in holders.items()],
        }

    def add_comment(self, asset_id, body, text):
        asset = STATE["assets"].get(asset_id)
        if not asset:
            return 404, {"message": text["asset-tree.error.notFound"]}
        note = ((body or {}).get("body") or "").strip()
        if not note:
            return 400, {"message": text["asset-tree.error.comment.required"]}
        if len(note) > MAX_COMMENT:
            return 400, {"message": text["asset-tree.error.description.length"]}
        comment_id = STATE["comment_seq"]
        STATE["comment_seq"] += 1
        row = {
            "id": comment_id, "assetId": asset_id, "authorKey": "ivanov",
            "body": note, "created": now_stamp(),
        }
        STATE["comments"][comment_id] = row
        log_activity(asset_id, "comment", "", "", note[:500])
        return 201, comment_dto(row)

    def delete_comment(self, asset_id, comment_id, text):
        row = STATE["comments"].get(comment_id)
        if not row or row["assetId"] != asset_id or asset_id not in STATE["assets"]:
            return 404, {"message": text["asset-tree.error.notFound"]}
        log_activity(asset_id, "comment_delete", "", row.get("body") or "", "")
        STATE["comments"].pop(comment_id, None)
        return 204, None

    def delete_file(self, asset_id, file_id, text):
        row = STATE["files"].get(file_id)
        if not row or row["assetId"] != asset_id or asset_id not in STATE["assets"]:
            return 404, {"message": text["asset-tree.error.notFound"]}
        log_activity(asset_id, "file_delete", "", row.get("fileName") or "", "")
        STATE["files"].pop(file_id, None)
        delete_preview_file(file_id)
        return 204, None

    def serve_file(self, method, query):
        text = self.text()
        if method == "GET":
            try:
                file_id = int((query.get("id") or ["0"])[0])
            except ValueError:
                file_id = 0
            with LOCK:
                row = STATE["files"].get(file_id)
                path = preview_file_path(file_id) if row else ""
            if not row or not os.path.isfile(path):
                self.send_error(404)
                return
            with open(path, "rb") as handle:
                data = handle.read()
            name = (row["fileName"] or "file").replace('"', "").replace("\r", "").replace("\n", "")
            ascii_name = name.encode("ascii", "replace").decode("ascii")
            disposition = 'attachment; filename="%s"; filename*=UTF-8\'\'%s' % (ascii_name, quote(name))
            self.respond(200, data, row["contentType"] or "application/octet-stream", {
                "X-Content-Type-Options": "nosniff",
                "Content-Disposition": disposition,
            })
            return
        if method != "POST":
            self.send_error(405)
            return
        try:
            asset_id = int((query.get("assetId") or ["0"])[0])
        except ValueError:
            asset_id = 0
        length = int(self.headers.get("Content-Length") or 0)
        if asset_id <= 0 or length <= 0 or length > MAX_FILE_BYTES + 65536:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.required"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        raw = self.rfile.read(length)
        parsed = parse_multipart(raw, self.headers.get("Content-Type", ""))
        if not parsed or not parsed[2]:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.required"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        name, content_type, data = parsed
        if len(data) > MAX_FILE_BYTES:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.size"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        safe = safe_file_name(name)
        if not safe:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.name"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        with LOCK:
            if asset_id not in STATE["assets"]:
                missing = True
                file_id = 0
            else:
                missing = False
                file_id = STATE["file_seq"]
                STATE["file_seq"] += 1
                STATE["files"][file_id] = {
                    "id": file_id, "assetId": asset_id, "fileName": safe,
                    "contentType": content_type or "application/octet-stream",
                    "size": len(data), "authorKey": "ivanov", "created": now_stamp(),
                }
        if missing:
            self.respond(404, json.dumps({"message": text["asset-tree.error.notFound"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        try:
            write_preview_file(file_id, data)
        except OSError:
            with LOCK:
                STATE["files"].pop(file_id, None)
            self.respond(500, json.dumps({"message": text["asset-tree.error.unexpected"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        with LOCK:
            log_activity(asset_id, "file", "", "", safe)
        self.respond(201, json.dumps({"id": file_id}).encode("utf-8"), "application/json; charset=utf-8")

    def serve_card_image(self, method, query):
        text = self.text()
        try:
            asset_id = int((query.get("assetId") or ["0"])[0])
        except ValueError:
            asset_id = 0
        if asset_id <= 0:
            self.send_error(400)
            return
        if method == "GET":
            with LOCK:
                known = asset_id in STATE["card_images"]
                kind = STATE["card_images"].get(asset_id) or "png"
            path = card_image_path(asset_id)
            if not known or not os.path.isfile(path):
                self.send_error(404)
                return
            with open(path, "rb") as handle:
                data = handle.read()
            content = "image/jpeg" if kind == "jpeg" else "image/png"
            self.respond(200, data, content, {"X-Content-Type-Options": "nosniff", "Cache-Control": "private, max-age=3600"})
            return
        if method == "DELETE":
            with LOCK:
                if asset_id not in STATE["assets"]:
                    missing = True
                    had = False
                else:
                    missing = False
                    had = asset_id in STATE["card_images"]
                    STATE["card_images"].pop(asset_id, None)
                    if had:
                        log_activity(asset_id, "card_image_delete", "", "1", "")
                        STATE["assets"][asset_id]["updated"] = now_stamp()
            if missing:
                self.respond(404, json.dumps({"message": text["asset-tree.error.notFound"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
                return
            delete_card_image(asset_id)
            self.respond(204, b"", "application/json; charset=utf-8")
            return
        if method != "POST":
            self.send_error(405)
            return
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0 or length > CARD_IMAGE_MAX + 65536:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.required"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        raw = self.rfile.read(length)
        parsed = parse_multipart(raw, self.headers.get("Content-Type", ""))
        if not parsed or not parsed[2]:
            self.respond(400, json.dumps({"message": text["asset-tree.error.file.required"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        data = parsed[2]
        if len(data) > CARD_IMAGE_MAX:
            self.respond(400, json.dumps({"message": text["asset-tree.error.cardImage.size"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        kind = card_image_kind(data)
        if not kind:
            self.respond(400, json.dumps({"message": text["asset-tree.error.cardImage.type"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        with LOCK:
            asset = STATE["assets"].get(asset_id)
            if not asset:
                self.respond(404, json.dumps({"message": text["asset-tree.error.notFound"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
                return
            if is_location_id(asset_id):
                self.respond(400, json.dumps({"message": text["asset-tree.error.cardImage.place"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
                return
        try:
            os.makedirs(FILE_DIR, exist_ok=True)
            with open(card_image_path(asset_id), "wb") as handle:
                handle.write(data)
        except OSError:
            self.respond(500, json.dumps({"message": text["asset-tree.error.unexpected"]}, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")
            return
        with LOCK:
            STATE["card_images"][asset_id] = kind
            STATE["assets"][asset_id]["updated"] = now_stamp()
            log_activity(asset_id, "card_image", "", "", "1")
        self.respond(201, json.dumps({"id": asset_id}).encode("utf-8"), "application/json; charset=utf-8")

    def read_json(self):
        length = int(self.headers.get("Content-Length") or 0)
        if length <= 0:
            return {}
        raw = self.rfile.read(length)
        return json.loads(raw.decode("utf-8")) if raw else {}

    def respond(self, status, body, content_type, extra=None):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        for key, value in (extra or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print("Asset tree preview at http://127.0.0.1:%s" % PORT)
    server.serve_forever()
