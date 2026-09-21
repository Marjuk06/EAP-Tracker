import React from 'react';

interface OverallProgressProps {
  completedChapters: number;
  totalChapters: number;
}

export const OverallProgress: React.FC<OverallProgressProps> = ({
  completedChapters,
  totalChapters
}) => {
  const percent = totalChapters > 0 ? Math.round((completedChapters / totalChapters) * 100) : 0;
  const progressRatio = totalChapters > 0 ? completedChapters / totalChapters : 0;

  // SVG parameters for 260 deg arc gauge
  const radius = 28;
  const strokeWidth = 5;
  const circumference = 2 * Math.PI * radius;
  // 260 deg fraction of 360 deg is 260 / 360 = 0.7222
  const arcLength = circumference * (260 / 360);
  const strokeDashoffset = arcLength - progressRatio * arcLength;

  return (
    <div
      className="haze-card"
      style={{
        padding: '16px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 16
      }}
    >
      <div>
        <div style={{ fontSize: 16, fontWeight: 700, color: '#e6e0e9' }}>
          Your Progress
        </div>
        <div style={{ fontSize: 13, color: 'rgba(230, 224, 233, 0.7)', marginTop: 4 }}>
          {completedChapters} / {totalChapters} chapters completed
        </div>
      </div>

      <div style={{ position: 'relative', width: 72, height: 72, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <svg width="72" height="72" viewBox="0 0 72 72">
          {/* Background Arc */}
          <circle
            cx="36"
            cy="36"
            r={radius}
            fill="none"
            stroke="rgba(255, 255, 255, 0.15)"
            strokeWidth={strokeWidth}
            strokeDasharray={`${arcLength} ${circumference}`}
            strokeLinecap="round"
            transform="rotate(140 36 36)"
          />
          {/* Progress Arc */}
          <circle
            cx="36"
            cy="36"
            r={radius}
            fill="none"
            stroke="#d0bcff"
            strokeWidth={strokeWidth + 1}
            strokeDasharray={`${arcLength} ${circumference}`}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            transform="rotate(140 36 36)"
            style={{ transition: 'stroke-dashoffset 0.8s cubic-bezier(0.16, 1, 0.3, 1)' }}
          />
        </svg>
        <div
          style={{
            position: 'absolute',
            fontFamily: 'var(--font-mono)',
            fontSize: 16,
            fontWeight: 800,
            color: '#e6e0e9'
          }}
        >
          {percent}%
        </div>
      </div>
    </div>
  );
};
