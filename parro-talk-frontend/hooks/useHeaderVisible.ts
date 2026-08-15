"use client";

import { useState, useEffect, useRef } from "react";

export function useHeaderVisible() {
  const [isVisible, setIsVisible] = useState(true);
  const lastScrollTop = useRef(0);

  useEffect(() => {
    const handleScroll = (event: Event) => {
      const target = event.target;
      if (!target) return;

      let scrollTop = 0;
      if (target === document || target === window) {
        scrollTop = window.scrollY || document.documentElement.scrollTop;
      } else if (target instanceof HTMLElement) {
        scrollTop = target.scrollTop;
      } else {
        return;
      }

      // Avoid triggering on negative values (iOS rubber band scroll)
      if (scrollTop < 0) return;

      const currentScrollTop = scrollTop;
      const difference = currentScrollTop - lastScrollTop.current;
      const threshold = 8; // Small threshold to avoid micro-scroll triggers

      // Always show header at the very top of the page
      if (currentScrollTop <= 50) {
        setIsVisible(true);
        lastScrollTop.current = currentScrollTop;
      } else if (Math.abs(difference) > threshold) {
        if (difference > 0) {
          // Scrolling down - hide header
          setIsVisible(false);
        } else {
          // Scrolling up - show header
          setIsVisible(true);
        }
        lastScrollTop.current = currentScrollTop;
      }
    };

    // Use capture phase (true) because scroll events do not bubble
    window.addEventListener("scroll", handleScroll, true);
    return () => {
      window.removeEventListener("scroll", handleScroll, true);
    };
  }, []);

  return isVisible;
}
