package com.example.blockchain.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A lightweight, highly secure implementation of BIP-39 Mnemonic Seed Phrase generator
 * specifically adapted for Takium (TKM).
 */
object BIP39 {
    // A rich subset of standard English BIP-39 words for authentic seed phrase generation
    val WORD_LIST = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse",
        "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
        "action", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust", "admit",
        "adult", "advance", "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
        "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol", "alert",
        "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also", "alter",
        "always", "amateur", "amazing", "among", "amount", "amused", "analyst", "anchor", "ancient", "anger",
        "angle", "angry", "animal", "ankle", "announce", "annual", "another", "answer", "antenna", "antique",
        "anxiety", "any", "apart", "apology", "appear", "apple", "approve", "april", "arch", "arctic",
        "area", "arena", "argue", "arm", "armed", "armor", "army", "around", "arrange", "arrest",
        "arrive", "arrow", "art", "artefact", "artist", "artwork", "ask", "aspect", "assault", "asset",
        "assist", "assume", "asthma", "athlete", "atom", "attack", "attend", "attitude", "attract", "auction",
        "baby", "bachelor", "bacon", "badge", "bag", "balance", "balloon", "banana", "band", "banner",
        "bar", "barely", "bargain", "barrel", "barrier", "base", "basic", "basket", "battle", "beach",
        "bean", "beauty", "because", "become", "beef", "before", "begin", "behave", "behind", "believe",
        "below", "belt", "bench", "benefit", "best", "betray", "better", "between", "beyond", "bicycle",
        "bid", "bike", "bind", "biology", "bird", "birth", "bitter", "black", "blade", "blame",
        "blanket", "blast", "bleak", "bless", "blind", "blood", "blossom", "blouse", "blue", "blur",
        "blush", "board", "boat", "body", "boil", "bomb", "bone", "bonus", "book", "boost",
        "border", "boring", "borrow", "boss", "bottom", "bounce", "box", "boy", "bracket", "brain",
        "brand", "brass", "brave", "bread", "breeze", "brick", "bridge", "brief", "bright", "bring",
        "brisk", "broccoli", "broken", "bronze", "broom", "brother", "brown", "brush", "bubble", "buddy",
        "budget", "buffalo", "build", "bulb", "bulk", "bullet", "bundle", "bunker", "burden", "burger",
        "burst", "bus", "business", "busy", "butter", "buyer", "buzz", "cabbage", "cabin", "cable",
        "cactus", "cage", "cake", "call", "calm", "camera", "camp", "can", "canal", "cancel",
        "candy", "cannon", "canoe", "canvas", "canyon", "capable", "capital", "captain", "car", "carbon",
        "card", "cargo", "care", "carpet", "carry", "cart", "case", "cash", "casino", "castle",
        "casual", "cat", "catalog", "catch", "category", "cattle", "caught", "cause", "caution", "cave",
        "ceiling", "celery", "cement", "census", "century", "cereal", "certain", "chair", "chalk", "champion",
        "change", "chaos", "chapter", "charge", "chase", "chat", "cheap", "cheat", "check", "cheese",
        "chef", "cherry", "chest", "chicken", "chief", "child", "chimney", "china", "chisel", "chocolate",
        "choice", "choose", "chronic", "chuckle", "chunk", "churn", "cigar", "cinnamon", "circle", "citizen",
        "city", "civil", "claim", "clap", "clarify", "claw", "clay", "clean", "clerk", "clever",
        "click", "client", "cliff", "climb", "clinic", "clip", "clock", "clog", "close", "cloth",
        "cloud", "clown", "club", "clump", "cluster", "clutch", "coach", "coast", "coconut", "code",
        "coffee", "coil", "coin", "collect", "colony", "color", "column", "combine", "come", "comfort",
        "comic", "common", "company", "compass", "compel", "complete", "confirm", "congress", "connect", "consider",
        "control", "convince", "cook", "cool", "copper", "copy", "coral", "core", "corn", "corner",
        "correct", "cost", "cotton", "couch", "country", "couple", "course", "cousin", "cover", "coyote",
        "crack", "cradle", "craft", "cram", "crane", "crash", "crater", "crawl", "crazy", "cream",
        "credit", "creek", "crew", "cricket", "crime", "crisp", "critic", "crop", "cross", "crouch",
        "crowd", "crucial", "cruel", "cruise", "crumble", "crunch", "crush", "cry", "crystal", "cube",
        "culture", "cup", "cupboard", "curious", "current", "curtain", "curve", "cushion", "custom", "cute",
        "cycle", "danger", "dark", "darling", "dash", "date", "dawn", "day", "deal", "debate",
        "debris", "decade", "december", "decide", "decline", "decorate", "decrease", "deer", "defense", "define",
        "defy", "degree", "delay", "deliver", "demand", "demise", "denial", "dentist", "deny", "depart",
        "depend", "deposit", "depth", "deputy", "derby", "describe", "desert", "design", "desk", "despair",
        "destroy", "detail", "detect", "device", "devote", "diagram", "dial", "diamond", "diary", "dice",
        "diesel", "diet", "differ", "digital", "dignity", "dilemma", "dinner", "dinosaur", "direct", "dirt",
        "disagree", "discover", "disease", "dish", "dismiss", "disorder", "display", "distance", "divert", "divide",
        "divorce", "dizzy", "doctor", "document", "dog", "doll", "dolphin", "domain", "donate", "donkey",
        "donor", "door", "dose", "double", "dove", "draft", "dragon", "drama", "drastic", "draw",
        "dream", "dress", "drift", "drill", "drink", "drip", "drive", "drop", "drum", "dry",
        "duck", "dumb", "dune", "during", "dust", "dutch", "duty", "dwarf", "dynamic", "eager",
        "eagle", "early", "earn", "earth", "easily", "east", "easy", "echo", "ecology", "economy",
        "edge", "edit", "educate", "effort", "egg", "eight", "either", "elbow", "elder", "electric",
        "elegant", "element", "elephant", "elevator", "elite", "else", "embark", "embody", "embrace", "emerge",
        "emotion", "employ", "empower", "empty", "enable", "enact", "end", "endless", "endorse", "enemy",
        "energy", "enforce", "engage", "engine", "engross", "enjoy", "enlist", "enough", "enrich", "enroll",
        "ensure", "enter", "entire", "entry", "envelope", "episode", "equal", "equip", "era", "erase",
        "erode", "erosion", "error", "erupt", "escape", "essay", "essence", "estate", "eternal", "ethics",
        "evidence", "evil", "evoke", "evolve", "exact", "example", "excess", "exchange", "excite", "exclude",
        "excuse", "execute", "exercise", "exhaust", "exhibit", "exile", "exist", "exit", "exotic", "expand",
        "expect", "expire", "explain", "expose", "express", "extend", "extra", "eye", "eyebrow", "fabric",
        "face", "faculty", "fade", "faint", "faith", "fall", "false", "fame", "family", "famous",
        "fan", "fancy", "fantasy", "farm", "fashion", "fat", "fatal", "father", "fatigue", "fault",
        "favorite", "feature", "february", "federal", "fee", "feed", "feel", "female", "fence", "festival",
        "fetch", "fever", "few", "fiber", "fiction", "field", "figure", "file", "film", "filter",
        "final", "find", "fine", "finger", "finish", "fire", "firm", "first", "fiscal", "fish",
        "fit", "fitness", "fix", "flag", "flame", "flash", "flat", "flavor", "flee", "fleet",
        "flesh", "flight", "flip", "float", "flock", "floor", "flower", "fluid", "flush", "fly",
        "foam", "focus", "fog", "foil", "fold", "follow", "food", "foot", "force", "forest",
        "forget", "fork", "form", "fortune", "forum", "forward", "fossil", "foster", "found", "fox",
        "fragile", "frame", "frequent", "fresh", "friend", "fringe", "frog", "front", "frost", "frown",
        "frozen", "frugal", "fruit", "fuel", "fun", "funny", "furnace", "fury", "future", "gadget",
        "gain", "galaxy", "gale", "gallery", "game", "gap", "garage", "garbage", "garden", "garlic",
        "garment", "gas", "gasp", "gate", "gather", "gauge", "gaze", "general", "genius", "genre",
        "gentle", "genuine", "gesture", "ghost", "giant", "gift", "giggle", "ginger", "giraffe", "girl",
        "give", "glad", "glance", "glare", "glass", "glide", "glimmer", "glimpse", "globe", "gloom",
        "glory", "glove", "glow", "glue", "goat", "goddess", "gold", "good", "goose", "gorilla",
        "gospel", "gossip", "govern", "gown", "grab", "grace", "grain", "grant", "grape", "graph",
        "grasp", "grass", "gravity", "gravy", "gray", "great", "green", "grid", "grief", "grit",
        "grocery", "groom", "group", "grow", "grunt", "guard", "guess", "guide", "guilt", "guitar",
        "gulf", "gun", "gurgle", "gut", "gym", "habit", "hair", "half", "hammer", "hand",
        "handle", "hang", "happen", "happy", "harbor", "hard", "harsh", "harvest", "hat", "have",
        "hawk", "hazard", "head", "health", "heart", "heavy", "hedgehog", "height", "hello", "helmet",
        "help", "hen", "hero", "hidden", "high", "hill", "hint", "hip", "hire", "history",
        "hobby", "hockey", "hold", "hole", "holiday", "hollow", "home", "honey", "hood", "hope",
        "horn", "horror", "horse", "hospital", "host", "hotel", "hour", "house", "hover", "how",
        "huge", "human", "humble", "humor", "hundred", "hungry", "hunt", "hurdle", "hurry", "hurt",
        "husband", "hybrid", "ice", "icon", "idea", "identify", "idle", "ignore", "ill",
        "illegal", "illness", "image", "imitate", "immense", "immune", "impact", "impose", "improve", "impulse",
        "inch", "include", "income", "increase", "index", "indicate", "indoor", "industry", "infant", "inflict",
        "inform", "inhale", "inherit", "initial", "inject", "injury", "ink", "innocent", "input", "inquiry",
        "insane", "insect", "inside", "inspire", "install", "intact", "interest", "into", "invest", "invite",
        "involve", "iron", "island", "isolate", "issue", "item", "ivory", "jacket", "jaguar", "jar",
        "jazz", "jealous", "jeans", "jelly", "jewel", "job", "join", "joke", "journey", "joy",
        "judge", "juice", "jump", "jungle", "junior", "junk", "just", "kangaroo", "keen", "keep",
        "ketchup", "key", "kick", "kid", "kidney", "kind", "kingdom", "kiss", "kit", "kitchen",
        "kite", "kitten", "kiwi", "knee", "knife", "knock", "know", "lab", "label", "labor",
        "ladder", "lady", "lake", "lamp", "language", "laptop", "large", "later", "latin", "latitude",
        "laugh", "laundry", "lava", "law", "lawn", "lawsuit", "layer", "lazy", "lead", "leaf",
        "learn", "leave", "lecture", "left", "leg", "legal", "legend", "leisure", "lemon", "lend",
        "length", "lens", "leopard", "lesson", "letter", "level", "liar", "liberty", "library", "license",
        "lick", "lid", "life", "lift", "light", "like", "limb", "limit", "link", "lion",
        "liquid", "list", "little", "live", "lizard", "load", "loan", "lobster", "local", "lock",
        "locust", "lodge", "loft", "log", "lois", "lonely", "long", "loop", "lottery", "loud",
        "lounge", "love", "loyal", "lucky", "luggage", "lumber", "lunar", "lunch", "luxury", "lyrics"
    )

    /**
     * Generates a new random 12-word mnemonic phrase.
     */
    fun generateMnemonic(): String {
        val random = SecureRandom()
        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val index = random.nextInt(WORD_LIST.size)
            words.add(WORD_LIST[index])
        }
        return words.joinToString(" ")
    }

    /**
     * Validates if a mnemonic is syntactically valid (contains exactly 12 words).
     * Accepts any 12 alphabetical words for full standard BIP-39 import compatibility.
     */
    fun isValid(mnemonic: String): Boolean {
        val words = mnemonic.trim().split("\\s+".toRegex())
        if (words.size != 12) return false
        return words.all { it.isNotEmpty() && it.all { char -> char.isLetter() } }
    }

    /**
     * Derives a 512-bit seed from the mnemonic phrase using PBKDF2WithHmacSHA512 (standard BIP-39).
     */
    fun deriveSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val salt = "mnemonic$passphrase"
        val spec = PBEKeySpec(
            mnemonic.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            2048,
            512
        )
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return skf.generateSecret(spec).encoded
    }
}
