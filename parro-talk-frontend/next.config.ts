import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/proxy/:path*",
        destination: `${process.env.BACKEND_URL || "http://20.194.32.60:8080/api"}/:path*`,
      },
    ];
  },
  allowedDevOrigins: ['192.168.1.107'],
};

export default nextConfig;

