# WhatsApp Guard — אפליקציית Android נייטיב (Kotlin)

אפליקציה המבצעת שתי משימות מרכזיות:

1. **ניטור "WhatsApp חי"** — מעקב אחר מתי WhatsApp הייתה פעילה לאחרונה (דרך התראות + נתוני שימוש), הצגת פרטי מכשיר וגרסאות, ושליחה תקופתית של הסטטוס לשרת Supabase.
2. **אימות/צימוד מחדש** — הזנת pair code, העתקה ל-clipboard, ו-PoC אוטומציה דרך AccessibilityService שמנסה להשלים את זרימת הצימוד ב-WhatsApp.

## מבנה המסכים

| מסך | קובץ | תפקיד |
|---|---|---|
| אימות | `ui/AuthScreen.kt` | התחברות/הרשמה מול Supabase Auth (אימייל + סיסמה) |
| סטטוס | `ui/StatusScreen.kt` | חותמת Alive אחרונה, פרטי מכשיר/גרסאות, שליחה ידנית לשרת, כפתור "בצע אימות מחדש" |
| אימות מחדש | `ui/PairScreen.kt` | TextBox ל-pair code + כפתור העתקה + כפתור "נסה אוטומציה" |

## רכיבים מרכזיים

- `data/SupabaseModule.kt` — חיבור Supabase (Auth + PostgREST), טבלת `device_status`.
- `monitor/WhatsAppMonitor.kt` — זיהוי פעילות WhatsApp אחרונה (התראות + UsageStats), גרסת WhatsApp מותקנת, פרטי מכשיר.
- `monitor/WaNotificationListener.kt` — NotificationListenerService שמסמן "חי" בכל התראת WhatsApp.
- `monitor/StatusSyncWorker.kt` — WorkManager ששולח סטטוס לשרת כל 15 דקות (מינימום WorkManager).
- `automation/PairingAccessibilityService.kt` — PoC אוטומציית צימוד (מכשירים מקושרים ← קשר מכשיר ← קשר עם מספר טלפון ← הדבקת הקוד).

## הגדרת Supabase

1. צור פרויקט ב-[supabase.com](https://supabase.com) והפעל Email Auth.
2. צור טבלה:

```sql
create table device_status (
  user_id uuid references auth.users primary key,
  device_model text,
  android_version text,
  app_version text,
  whatsapp_version text,
  whatsapp_last_alive bigint,
  updated_at bigint
);

alter table device_status enable row level security;
create policy "own rows" on device_status
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
```

3. החלף ב-`app/build.gradle.kts` את `SUPABASE_URL` ו-`SUPABASE_ANON_KEY`.

## הרשאות שהמשתמש צריך להפעיל

- **Usage Access** (הגדרות ← גישה לנתוני שימוש) — לחותמת פעילות אחרונה של WhatsApp.
- **גישה להתראות** (Notification Listener) — לסימון "חי" בזמן אמת.
- **שירות נגישות** — לאוטומציית הצימוד (PoC).

## הערות חשובות

- ה-PoC של האוטומציה תלוי בטקסטים של ממשק WhatsApp (עברית/אנגלית) ועלול לדרוש עדכון מחרוזות בין גרסאות WhatsApp. בחרתן/אחסנן ייתכנו שינויים.
- אוטומציה של אפליקציית צד-שלישי באמצעות Accessibility חייבת לעמוד במדיניות Google Play — שימוש אחראי ולמטרות לגיטימיות בלבד.
- בנייה: פתח את התיקייה ב-Android Studio (Hedgehog+) וסנכרן Gradle.

## בנייה

```
./gradlew assembleDebug
```
