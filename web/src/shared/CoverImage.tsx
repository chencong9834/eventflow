import { useState } from "react";

export function CoverImage({ src, title, height = 168 }: { src?: string; title: string; height?: number }) {
  const [failed, setFailed] = useState(false);
  if (src && !failed) {
    return (
      <img
        src={src}
        alt={title}
        className="cover-image"
        style={{ height }}
        onError={() => setFailed(true)}
      />
    );
  }
  const letter = (title || "E").trim().charAt(0).toUpperCase();
  return (
    <div className="cover-fallback" style={{ height }} aria-hidden>
      {letter}
    </div>
  );
}
