import React, { useState } from 'react';
import { Quote, Shuffle, Copy, Check } from 'lucide-react';
import { motivationalQuotes } from '../data/initialData';
import { sound } from '../services/audio';

export const MotivationalCard: React.FC = () => {
  const [index, setIndex] = useState(0);
  const [copied, setCopied] = useState(false);

  const current = motivationalQuotes[index % motivationalQuotes.length];

  const handleNext = () => {
    sound.playTick();
    setIndex((prev) => (prev + 1) % motivationalQuotes.length);
  };

  const handleCopy = () => {
    sound.playTick();
    navigator.clipboard.writeText(`"${current.quote}" - ${current.author}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      className="glass-card"
      style={{
        padding: '20px 24px',
        background: 'linear-gradient(135deg, rgba(22, 28, 44, 0.7), rgba(30, 25, 45, 0.6))',
        position: 'relative'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 14 }}>
        <div style={{ display: 'flex', gap: 12, flex: 1 }}>
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'rgba(255, 255, 255, 0.08)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-cyan)',
              flexShrink: 0
            }}
          >
            <Quote size={16} />
          </div>
          <div style={{ flex: 1 }}>
            <p
              style={{
                margin: '0 0 6px 0',
                fontSize: 14.5,
                lineHeight: 1.5,
                color: '#fff',
                fontStyle: 'italic'
              }}
            >
              "{current.quote}"
            </p>
            <div style={{ fontSize: 12, color: 'var(--accent-cyan)', fontWeight: 600 }}>
              — {current.author}
            </div>
          </div>
        </div>

        {/* Buttons */}
        <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
          <button
            onClick={handleCopy}
            title="Copy Quote"
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid var(--border-glass)',
              color: copied ? 'var(--accent-emerald)' : 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            {copied ? <Check size={14} /> : <Copy size={14} />}
          </button>

          <button
            onClick={handleNext}
            title="New Quote"
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid var(--border-glass)',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            <Shuffle size={14} />
          </button>
        </div>
      </div>
    </div>
  );
};
