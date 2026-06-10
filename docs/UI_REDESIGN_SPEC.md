# Catat UI Redesign — Apple-Inspired Visual Spec

Design language: Clean, minimalist, sleek — inspired by iOS 17 Health, Things 3, Day One.

---

## Design Tokens

### Typography (Inter font — closest to SF Pro)

| Token | Size | Weight | Letter Spacing | Line Height |
|-------|------|--------|---------------|-------------|
| LargeTitle | 34sp | Bold | 0.37 | 41sp |
| Title1 | 28sp | Bold | 0.34 | 34sp |
| Title2 | 22sp | Bold | 0.35 | 28sp |
| Title3 | 20sp | Semibold | 0.38 | 25sp |
| Body | 17sp | Regular | -0.43 | 22sp |
| Callout | 16sp | Regular | -0.32 | 21sp |
| Subheadline | 15sp | Semibold | -0.24 | 20sp |
| Footnote | 13sp | Regular | -0.08 | 18sp |
| Caption1 | 12sp | Regular | 0 | 16sp |
| Caption2 | 11sp | Regular | 0.07 | 13sp |

### Color Palette

```
Light Mode:
  background:        #F2F2F7   (system group table bg)
  cardBackground:    #FFFFFF
  primaryText:       #1C1C1E
  secondaryText:     #3A3A3C
  tertiaryText:      #8A8A8E
  separator:         #C6C6C8
  accent:            #0A84FF
  destructive:       #FF453A
  success:           #30D158
  warning:           #FFD60A

Dark Mode:
  background:        #1C1C1E
  cardBackground:    #2C2C2E
  primaryText:       #FFFFFF
  secondaryText:     #AEAEB2
  tertiaryText:      #636366
  separator:         #38383A
  accent:            #0A84FF
```

### Corner Radius
- xs: 6dp (badges, small buttons)
- sm: 10dp (list cards)
- md: 12dp (standard cards)
- lg: 16dp (modal sheets)
- xl: 20dp (floating button, pills)
- full: 999dp (circular)

### Shadows (iOS soft style)
- shadowSm: Offset(0,1), blur 3, black 12%
- shadowMd: Offset(0,2), blur 6, black 15%
- shadowLg: Offset(0,4), blur 12, black 18%
- shadowXl: Offset(0,8), blur 24, black 22%

### Animation Curves
- springStiff: dampingRatio 0.7, stiffness 200 (cards appearing)
- springBouncy: dampingRatio 0.6, stiffness 150 (sheets presenting)
- tweenDefault: 350ms, FastOutSlowInEasing
- staggerDelay: 50ms per list item

---

## Screen-by-Screen Specs

### 1. SplashScreen
- Full white background
- Center: custom camera aperture icon (80dp), gradient #0A84FF → #0063CE, soft glow shadow
- App name "Catat" — LargeTitle 34sp bold, 16dp below icon
- Subtitle "Bug Report Companion" — Footnote 13sp tertiaryText
- Bottom: 3 bouncing dots (like iMessage typing), 6dp each, sequential bounce 0.3s loop

### 2. OnboardingScreen (3 pages)
- "Skip" text button top-right (Caption1)
- Illustration area (55% height): 2-color + accent line art, radial gradient background
- Title: Title2 22sp bold, centered
- Body: Body 17sp, centered, secondaryText, 24dp horizontal margin
- Page dots: 8dp, active = accent, inactive = separator, spring transition
- Button: pill shape 120dp, accent fill, white text "Next", Page 3 shows "Get Started"
- Parallax: illustration moves 30% slower

### 3. HistoryScreen
- Search pill: 36dp height, cardBackground, magnifying glass icon 16dp, hint "Search Reports"
- Title: LargeTitle 34sp bold "Reports"
- Filter chips: 32dp height, pill shape, horizontal scroll. Selected = accent fill. All = accent border
- Report cards: 12dp radius, 72dp height, shadowSm
  - Left: thumbnail 56dp square 8dp radius
  - Center: title (Title3) + subtitle (Footnote secondaryText) + status badge
  - Right: chevron 12dp tertiaryText
- Stagger appear: 50ms delay per card, fade + slide up 20dp
- Empty state: custom illustration 80dp + "No Reports Yet" Title1 + body text + "[? How to use]" link
- FAB: 56dp circle, accent fill, white "+", shadowLg, bottom-right

### 4. AnnotationScreen
- Full-screen black background
- Top bar: transparent, 46dp, X icon left white, "Annotate" center, "Done" accent right
- Canvas: black bg, screenshot CenterInside, pinch zoom max 3x, pan when zoomed
- Floating tool pill: bottom, 44dp height, #2C2C2E 0.95 opacity, pill shape, shadowLg
  - 5 tool icons 26dp + undo/redo 14dp + color indicator 16dp circle
- Color picker expandable: 48dp, same pill style, 6 presets (22dp) + custom gradient + stroke width dots

### 5. ReportDetailScreen
- Grouped iOS form style
- Screenshot card: 12dp radius, 8dp margin, tap for fullscreen overlay
- Form fields: each in card with uppercase section label (13sp), 17sp body input
  - Title: single-line, no border, bottom accent line on focus (0.2s width animation)
  - Steps to Reproduce: multi-line 4 lines min + mic icon trailing
  - Actual/Expected: 2-column grid, 4dp gap
- Device Context card: compact 2-column grid, F2F2F7 bg, edit button top-right
- Auto-save indicator: "✓ Saved 2:30PM" success green, 3s visible

### 6. ExportScreen
- Format grid: 2x2, 80dp x 64dp each, 12dp radius
  - Selected: accent fill white text, Unselected: cardBackground accent border
  - Icon top 20dp + label bottom 13sp
- Preview panel: 12dp radius, #FAFAFA bg, 15sp monospace, max 40% height, scrollable
- Action buttons: 3 equal width, 50dp height, 12dp radius
  - Copy: accent outlined, Share: accent filled, Save: tertiary bg outlined
- Snackbar: iOS notification style (TOP position), slides down, 3s auto-dismiss

### 7. SettingsScreen
- iOS Settings replica — grouped table style
- Section headers: Footnote 13sp uppercase, 8dp margin top
- Card group: continuous 12dp radius
- Each row: 44dp height, divider 1dp separator 16dp inset
- Label left (Body 17sp), value right (Callout tertiary + chevron)
- Toggle rows: iOS style toggle (accent fill, white thumb, 0.2s spring)
- Destructive row: body 17sp #FF453A
- Last row no divider per group

---

## Micro-interactions

1. Card press → scale 0.97 + opacity 0.9 → release → spring back
2. List appear → 50ms staggered fade + slide up 20dp
3. Modal present → slide up + background fade to overlayDark
4. Snackbar → slide down from TOP (like iOS notification)
5. Toggle → track fill animation + thumb slide (0.2s spring)
6. Navigation → crossfade content
7. Button press → scale 0.95 spring back
8. Search → expand with content appearing below
9. Swipe delete → red background reveal + trash icon
