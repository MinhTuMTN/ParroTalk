function isYoutubeUrl(url: string): boolean {
    if (!url?.trim()) {
        return false;
    }

    try {
        const { hostname } = new URL(url.trim());

        const host = hostname.toLowerCase();

        return [
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be",
            "www.youtu.be",
        ].includes(host);
    } catch {
        return false;
    }
}

export function classifyUrl(url: string): string {
    if (isYoutubeUrl(url)) {
        return "YOUTUBE";
    }

    return "AUDIO"
}