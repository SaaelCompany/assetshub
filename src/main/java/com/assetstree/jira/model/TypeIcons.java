package com.assetstree.jira.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Icon keys a type can carry. The glyphs themselves live in the page script;
 * the server only stores and validates the key so old rows and unknown values
 * fall back to a sensible default.
 */
public final class TypeIcons {
    public static final String DEFAULT_PLACE = "building";
    public static final String DEFAULT_OBJECT = "device";

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            "building", "warehouse", "department", "office", "hospital", "factory", "store", "home",
            "device", "desktop", "laptop", "monitor", "server", "printer", "scanner", "phone", "tablet",
            "camera", "network", "wifi", "storage", "keyboard", "projector", "battery",
            "medical", "microscope", "tool", "vehicle", "furniture", "box", "document", "tag",
            "project", "megaphone", "target", "bars", "headset", "users", "chat", "clipboard",
            "gear", "badge", "syringe", "pill", "pulse", "code", "window",
            "officebuilding", "hospitalbuilding", "homecity", "domain", "storefront", "school", "bank",
            "beddouble", "parking", "garage", "floorplan", "mapmarker", "door", "townhall", "greenhouse",
            "homegroup", "stadium", "church", "tent", "desktoptower", "desktopclassic", "monitormultiple",
            "monitordashboard", "servernetwork", "database", "nas", "harddisk", "usbflashdrive", "memory",
            "chip", "routerwireless", "routernetwork", "lan", "accesspoint", "ethernet", "cabledata",
            "printer3d", "fax", "cellphone", "phoneclassic", "phonevoip", "webcam", "cctv", "mouse",
            "projectorscreen", "television", "speaker", "microphone", "batterycharging", "powerplug",
            "cloud", "shieldlock", "key", "sim", "console", "application", "barcodescan", "qrcodescan",
            "cardaccountdetails", "pointofsale", "watch", "headphones", "gamepadvariant", "usb", "antenna",
            "satelliteuplink", "hospitalbox", "stethoscope", "heartpulse", "medicalbag", "needle",
            "bottletonicplus", "testtube", "flask", "thermometer", "wheelchair", "bed", "ambulance", "tooth",
            "eye", "lungs", "brain", "dna", "virus", "bandage", "mothernurse", "doctor", "facemask",
            "radioactive", "radiologybox", "medication", "ivbag", "diabetes", "scalebathroom", "bloodbag",
            "humanmaleheight", "babycarriage", "handwash", "clipboardpulse", "clipboardplus", "gascylinder",
            "medcross", "heart", "bone", "earhearing", "humanpregnant", "allergy", "biohazard", "monitoreye",
            "waterpercent", "scalebalance", "sprinkler", "foodapple", "walk", "humanwheelchair",
            "airconditioner", "fan", "hvac", "radiator", "thermostat", "waterboiler", "waterpump", "pipe",
            "pipevalve", "water", "lightbulb", "ceilinglight", "flash", "transmissiontower", "powersocketeu",
            "electricswitch", "fuse", "meterelectric", "gauge", "fireextinguisher", "fire", "smokedetector",
            "alarmlight", "elevator", "escalator", "stairs", "doorclosedlock", "wrench", "hammerwrench",
            "screwdriver", "toolbox", "ladder", "broom", "trashcan", "recycle", "forklift", "truck", "car",
            "engine", "solarpanel", "washingmachine", "fridge", "stove", "coffeemaker", "microwave",
            "shower", "toilet", "snowflake", "boomgate", "shieldhome", "sawblade", "hammer", "pail", "valve",
            "waterboileralert", "airfilter", "airpurifier", "windowclosedvariant", "blinds", "spraybottle",
            "vacuum", "transmissiontowerexport", "homethermometer", "lock", "keyvariant", "bullhorn",
            "chartbar", "chartline", "chartpie", "trendingup", "video", "videovintage", "movieopen",
            "palette", "brush", "draw", "image", "tshirtcrew", "gift", "tagmultiple", "sale", "newspaper",
            "email", "emailnewsletter", "web", "star", "thumbup", "sharevariant", "lightbulbon",
            "presentation", "calendarstar", "flag", "ticket", "bookopenvariant", "cardtext", "pencilruler",
            "formatpaint", "billboard", "partypopper", "trophy", "medal", "crown", "camerairis",
            "lightbulbgroup", "drone", "printerpos", "signtext", "bookopenpagevariant", "post",
            "rocketlaunch", "faceagent", "phoneintalk", "forum", "messagetext", "accountgroup", "accounttie",
            "handshake", "cashregister", "receipt", "creditcard", "ticketconfirmation", "calendarclock",
            "clock", "timersand", "emoticonhappy", "commentquote", "information", "helpcircle", "lifebuoy",
            "bellring", "desk", "chairrolling", "sofa", "ticketaccount", "monitoraccount", "accountvoice",
            "cardaccountphone", "contactlesspayment", "handcoin", "smartcardreader", "wateroutline",
            "coffee", "umbrella", "hanger", "tablefurniture", "filecabinet", "archive", "packagevariant",
            "briefcase", "folder", "book", "calculator", "safe", "clipboardtext", "paperclip", "pen",
            "desklamp", "calendar", "flower", "cart", "bookshelf", "wardrobe", "shredder", "scissorscutting",
            "stamper", "certificate", "seal", "bicycle", "bus"
    ));

    private TypeIcons() {
    }

    public static boolean isIcon(String icon) {
        return icon != null && ALL.contains(icon);
    }

    public static String defaultFor(boolean location) {
        return location ? DEFAULT_PLACE : DEFAULT_OBJECT;
    }

    /** Returns the stored key when it is known, otherwise the default for the kind of type. */
    public static String resolve(String stored, boolean location) {
        String key = stored == null ? "" : stored.trim().toLowerCase(Locale.ROOT);
        return isIcon(key) ? key : defaultFor(location);
    }
}
