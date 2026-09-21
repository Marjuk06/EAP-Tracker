import React, { useState } from 'react';
import { ArrowLeft, GraduationCap, BookOpen } from 'lucide-react';
import { RoutineItem } from '../types';
import { sound } from '../services/audio';

interface FullRoutineScreenProps {
  type: string;
  routines: RoutineItem[];
  onBack: () => void;
}

export const FullRoutineScreen: React.FC<FullRoutineScreenProps> = ({
  type,
  routines,
  onBack
}) => {
  const [showTopics, setShowTopics] = useState(true);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, width: '100%', paddingBottom: 60 }}>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 0' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button
            onClick={() => {
              sound.playTick();
              onBack();
            }}
            style={{
              width: 42,
              height: 42,
              borderRadius: '50%',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              background: 'rgba(255, 255, 255, 0.06)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer'
            }}
          >
            <ArrowLeft size={20} />
          </button>

          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
            {type.toUpperCase()} ROUTINE
          </h2>
        </div>

        <button
          onClick={() => {
            sound.playTick();
            setShowTopics((prev) => !prev);
          }}
          style={{
            padding: '8px 16px',
            borderRadius: showTopics ? '20px' : '8px',
            border: 'none',
            background: showTopics ? '#d0bcff' : 'rgba(255, 255, 255, 0.15)',
            color: showTopics ? '#381e72' : '#fff',
            fontSize: 13,
            fontWeight: 800,
            cursor: 'pointer',
            transition: 'all 0.2s ease'
          }}
        >
          Toggle Topics
        </button>
      </div>

      {/* Routine Cards Grid (Responsive 2-col on desktop) */}
      <div className="responsive-items-grid">
        {routines.map((item) => (
          <div
            key={item.id}
            className="haze-card"
            style={{
              padding: '18px 20px',
              borderRadius: '24px'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
              {/* Date Badge */}
              <div
                style={{
                  padding: '8px 10px',
                  borderRadius: '12px',
                  background: 'rgba(208, 188, 255, 0.12)',
                  textAlign: 'center',
                  minWidth: 68,
                  flexShrink: 0
                }}
              >
                <div style={{ fontSize: 13.5, fontWeight: 800, color: '#d0bcff' }}>{item.date}</div>
                <div style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.6)' }}>{item.day}</div>
              </div>

              {/* Lecture & Exam details */}
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
                  {item.classSubject && (
                    <span
                      style={{
                        fontSize: 11.5,
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '6px',
                        background: 'rgba(208, 188, 255, 0.2)',
                        color: '#d0bcff',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4
                      }}
                    >
                      <GraduationCap size={12} />
                      {item.classSubject}
                    </span>
                  )}

                  {item.examDetails && (
                    <span
                      style={{
                        fontSize: 11.5,
                        fontWeight: 700,
                        padding: '2px 8px',
                        borderRadius: '6px',
                        background: 'rgba(255, 213, 79, 0.2)',
                        color: '#ffd54f',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 4
                      }}
                    >
                      <BookOpen size={12} />
                      {item.examDetails}
                    </span>
                  )}
                </div>

                {showTopics && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {item.topics.map((top, idx) => (
                      <div key={idx} style={{ fontSize: 13, color: 'rgba(255,255,255,0.85)', lineHeight: 1.35 }}>
                        • {top}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
