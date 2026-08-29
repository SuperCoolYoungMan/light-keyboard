# Korean keyboard haptic tuning

## Goal

Create short, precise tactile feedback with minimal rumble. The keyboard should communicate state through shape and timing, not raw vibration strength.

## Reference profile: Crisp

| Event | Primitive composition | Intent |
| --- | --- | --- |
| Character | `LOW_TICK 0.24` | Nearly invisible confirmation at typing speed |
| Space | `CLICK 0.30` + `QUICK_FALL 0.12` | Slightly deeper boundary between words |
| Shift | `TICK 0.28` + `QUICK_RISE 0.14` | State change without feeling heavy |
| Backspace | `LOW_TICK 0.27` | Clear deletion feedback |
| Backspace repeat | `LOW_TICK 0.14` | Low-fatigue feedback during accelerated deletion |
| Enter | `THUD 0.24` + 6 ms + `CLICK 0.22` | Compact confirmation / completion |
| Korean ↔ English | `LOW_TICK 0.25` + 24 ms + `CLICK 0.34` | Two-stage state transition that is recognizable without looking |
| Long press | `QUICK_RISE 0.14` + 4 ms + `CLICK 0.22` | Confirms that the gesture crossed the long-press threshold |

Strength presets multiply primitive scale while preserving the timing/shape of the profile:

- Off: disabled
- Light: 0.68×
- Crisp: 1.00× reference
- Strong: 1.25×, clamped to Android's primitive scale range

## Capability fallback

At runtime the settings screen reports support for `LOW_TICK`, `TICK`, `CLICK`, `THUD`, `QUICK_RISE`, and `QUICK_FALL`.

1. Use the composed profile when every primitive required by the event is supported.
2. Otherwise use Android's device-tuned predefined effect.
3. If that is explicitly unsupported, use a very short one-shot/waveform fallback.

## LP3 hardware tuning procedure

When testing on a real Light Phone III:

1. Start with `Crisp`.
2. Type continuously for 2–3 minutes; character feedback should be felt but not become a buzz.
3. Compare Space against Character. Space should feel slightly deeper, never longer or mushy.
4. Confirm Korean ↔ English can be recognized eyes-free from its two-stage pattern.
5. Hold Backspace for several seconds. Repeated feedback must remain subtle as deletion accelerates.
6. Test Enter and long press last; they should be distinguishable without overpowering ordinary typing.
7. Adjust primitive scale first. Change timing only if the LP3 actuator cannot separate the two stages cleanly.

The target is a crisp attack and fast decay. Avoid increasing duration as a substitute for actuator strength.
