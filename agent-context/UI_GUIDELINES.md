# UI_GUIDELINES.md — TabletTime

## Overall feel
Should feel like a small, well-crafted single-purpose app — think a polished alarm-clock or sleep app — not an enterprise form and not a default Compose demo.

## Color
- One clear, calm accent color (e.g. soft teal or sage green) — avoid the default Compose purple/teal template colors.
- Neutral, uncluttered backgrounds.
- Full dark mode support using the same accent, tuned for dark backgrounds — not just an inverted palette.

## Layout & spacing
- Generous whitespace — don't cram screens.
- Rounded corners on cards and buttons (12–20dp).
- Soft elevation/shadow on cards rather than hard borders.
- Big, obvious tap targets — especially the Yes/No buttons on the check-in screen (this may be used half-asleep or in a hurry).

## Typography
- Clear hierarchy: a large, friendly headline for the check-in question, medium weight for reminder labels, light/secondary weight for metadata (times, days).
- No dense paragraphs — every screen should be readable at a glance.

## Motion
- Toggles animate smoothly (`animateXAsState`).
- List additions/removals animate in/out (`AnimatedVisibility`).
- Screen transitions use gentle fades, not hard cuts.
- The check-in screen should feel calm, not jarring — a soft pulse or fade-in rather than a sudden pop-up.

## Copy / tone
- Short, warm, human. "Have you eaten your breakfast?" not "Confirm meal completion status."
- Confirmation messages should feel encouraging: "✅ Please take your tablet now" rather than clinical phrasing.

## Things to explicitly avoid
- Default unstyled Material components left in place anywhere.
- Dense, form-like screens.
- Harsh reds/alarms colors for normal UI — reserve strong color only for the actual alarm state.
- Any visual clutter (badges, counters, stats) not called for in `FEATURES.md`.
