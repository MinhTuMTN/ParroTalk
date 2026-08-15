import { COLORS, THEME_COLOR } from "@/lib/constants/colors";
import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "ParroTalk - English Dictation Practice",
    short_name: "ParroTalk",
    description:
      "Practice English listening with real videos, dictation, instant checking, Vietnamese translations, and progress tracking.",
    start_url: "/",
    scope: "/",
    display: "standalone",
    background_color: COLORS.background.page,
    theme_color: THEME_COLOR,
    icons: [
      {
        src: "/logo.png",
        sizes: "192x192",
        type: "image/png",
      },
      {
        src: "/logo.png",
        sizes: "512x512",
        type: "image/png",
      },
    ],
  };
}
