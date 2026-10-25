import type { CSSProperties, ReactNode } from "react";
import {
  AbsoluteFill,
  CanvasImage,
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { clamp, palette, sans, serif } from "./theme";
export const Stage = ({
  children,
  light = false,
}: {
  children: ReactNode;
  light?: boolean;
}) => (
  <AbsoluteFill
    style={{
      background: light ? palette.paper : palette.ground,
      color: light ? palette.ground : palette.paper,
      fontFamily: sans,
      overflow: "hidden",
    }}
  >
    {children}
  </AbsoluteFill>
);
export const Brand = ({ light = false }: { light?: boolean }) => {
  const { fps } = useVideoConfig();
  return (
    <Interactive.Div
      name="ReelVault signature"
      premountFor={fps}
      style={{
        position: "absolute",
        top: 100,
        left: 84,
        display: "flex",
        alignItems: "center",
        gap: 18,
        color: light ? palette.ground : palette.paper,
      }}
    >
      <CanvasImage
        src={staticFile("icon.svg")}
        premountFor={fps}
        style={{ width: 64, height: 64, borderRadius: 16 }}
      />
      <span
        style={{
          fontFamily: serif,
          fontSize: 44,
          fontWeight: 600,
          letterSpacing: -1.5,
        }}
      >
        ReelVault
      </span>
    </Interactive.Div>
  );
};
export const Headline = ({
  children,
  light = false,
  top = 220,
  size = 108,
}: {
  children: ReactNode;
  light?: boolean;
  top?: number;
  size?: number;
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Interactive.Div
      name="Scene headline"
      premountFor={fps}
      style={{
        position: "absolute",
        left: 84,
        right: 84,
        top,
        color: light ? palette.ground : palette.paper,
        fontFamily: serif,
        fontSize: size,
        fontWeight: 600,
        lineHeight: 1.04,
        letterSpacing: -3.7,
        translate: interpolate(
          frame,
          [0, 0.65 * fps],
          ["0px 90px", "0px 0px"],
          { ...clamp, easing: Easing.spring({ damping: 16 }) },
        ),
        opacity: interpolate(frame, [0, 0.25 * fps], [0, 1], clamp),
      }}
    >
      {children}
    </Interactive.Div>
  );
};
export const Phone = ({
  children,
  style,
}: {
  children: ReactNode;
  style?: CSSProperties;
}) => (
  <div
    style={{
      position: "absolute",
      left: 250,
      top: 530,
      width: 580,
      height: 1171,
      padding: 9,
      borderRadius: 52,
      background: "#080808",
      border: "2px solid #484641",
      boxShadow: "0 40px 70px #0005",
      ...style,
    }}
  >
    <div
      style={{
        position: "relative",
        width: "100%",
        height: "100%",
        overflow: "hidden",
        borderRadius: 42,
        background: palette.ground,
      }}
    >
      {children}
    </div>
  </div>
);
export const Footnote = ({
  children,
  light = false,
}: {
  children: ReactNode;
  light?: boolean;
}) => (
  <div
    style={{
      position: "absolute",
      bottom: 100,
      left: 84,
      right: 84,
      fontFamily: sans,
      fontSize: 44,
      letterSpacing: -1.2,
      color: light ? "#66635F" : palette.muted,
    }}
  >
    {children}
  </div>
);
