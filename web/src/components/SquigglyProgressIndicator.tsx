import React, { useRef, useEffect } from 'react';

interface SquigglyProgressIndicatorProps {
  progress: number; // 0.0 to 1.0
  color?: string;
  trackColor?: string;
  strokeWidth?: number;
  waveAmplitude?: number;
  waveLength?: number;
}

export const SquigglyProgressIndicator: React.FC<SquigglyProgressIndicatorProps> = ({
  progress,
  color = '#d0bcff',
  trackColor = 'rgba(255, 255, 255, 0.15)',
  strokeWidth = 3.5,
  waveAmplitude = 1.8,
  waveLength = 14
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;

    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, rect.width, rect.height);

    const width = rect.width;
    const centerY = rect.height / 2;
    const clampedProgress = Math.min(1, Math.max(0, progress));

    // Draw Track Path (Wavy background)
    ctx.beginPath();
    ctx.strokeStyle = trackColor;
    ctx.lineWidth = strokeWidth;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    ctx.moveTo(0, centerY);
    for (let x = 0; x <= width; x += 1.5) {
      const y = centerY + waveAmplitude * Math.sin((x / waveLength) * 2 * Math.PI);
      ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Draw Progress Path (Wavy foreground)
    if (clampedProgress > 0) {
      const progressWidth = width * clampedProgress;
      ctx.beginPath();
      ctx.strokeStyle = color;
      ctx.lineWidth = strokeWidth + 0.5;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';

      ctx.moveTo(0, centerY);
      for (let x = 0; x <= progressWidth; x += 1.5) {
        const y = centerY + waveAmplitude * Math.sin((x / waveLength) * 2 * Math.PI);
        ctx.lineTo(x, y);
      }
      ctx.stroke();
    }
  }, [progress, color, trackColor, strokeWidth, waveAmplitude, waveLength]);

  return (
    <canvas
      ref={canvasRef}
      style={{
        width: '100%',
        height: '10px',
        display: 'block'
      }}
    />
  );
};
