import React from 'react';
import { ChevronDown, Zap, FlaskConical, Calculator, Sparkles } from 'lucide-react';
import { Subject, Paper } from '../types';
import { SquigglyProgressIndicator } from './SquigglyProgressIndicator';
import { sound } from '../services/audio';

interface SyllabusSplitButtonProps {
  subject: Subject;
  isExpanded: boolean;
  onToggle: () => void;
  onPaperClick: (paper: Paper) => void;
}

export const SyllabusSplitButton: React.FC<SyllabusSplitButtonProps> = ({
  subject,
  isExpanded,
  onToggle,
  onPaperClick
}) => {
  let totalChapters = 0;
  let completedChapters = 0;

  subject.papers.forEach((p) => {
    p.chapters.forEach((c) => {
      totalChapters++;
      if (c.sections.every((s) => s.isCompleted)) {
        completedChapters++;
      }
    });
  });

  const progress = totalChapters > 0 ? completedChapters / totalChapters : 0;
  const percentInt = Math.round(progress * 100);

  const getSubjectIcon = (name: string) => {
    switch (name.toLowerCase()) {
      case 'physics':
        return <Zap size={22} color="#d0bcff" />;
      case 'chemistry':
        return <FlaskConical size={22} color="#d0bcff" />;
      case 'higher math':
        return <Calculator size={22} color="#d0bcff" />;
      case 'biology':
        return <Sparkles size={22} color="#d0bcff" />;
      default:
        return <Zap size={22} color="#d0bcff" />;
    }
  };

  return (
    <div
      className="haze-card"
      style={{
        padding: 16,
        marginBottom: 10,
        transition: 'all 0.25s ease'
      }}
    >
      {/* Clickable Header */}
      <div
        onClick={() => {
          sound.playTick();
          onToggle();
        }}
        style={{
          display: 'flex',
          alignItems: 'center',
          cursor: 'pointer',
          userSelect: 'none'
        }}
      >
        <div
          style={{
            width: 40,
            height: 40,
            borderRadius: 12,
            background: 'rgba(208, 188, 255, 0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginRight: 14,
            flexShrink: 0
          }}
        >
          {getSubjectIcon(subject.name)}
        </div>

        <div style={{ flex: 1 }}>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#fff' }}>
            {subject.name}
          </h3>
        </div>

        <div
          style={{
            transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
            color: 'rgba(230, 224, 233, 0.7)'
          }}
        >
          <ChevronDown size={22} />
        </div>
      </div>

      {/* Progress Info Row */}
      <div style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
          <span style={{ fontSize: 13, color: 'rgba(230, 224, 233, 0.7)' }}>
            {completedChapters} of {totalChapters} chapters
          </span>
          <span style={{ fontSize: 14, fontWeight: 700, color: '#d0bcff' }}>
            {percentInt}%
          </span>
        </div>

        <SquigglyProgressIndicator
          progress={progress}
          color="#d0bcff"
          trackColor="rgba(255, 255, 255, 0.12)"
        />
      </div>

      {/* Expanded Papers (1st Paper / 2nd Paper button group) */}
      {isExpanded && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 8,
            marginTop: 14,
            paddingTop: 12,
            borderTop: '0.5px solid rgba(255, 255, 255, 0.1)'
          }}
        >
          {subject.papers.map((paper) => {
            const done = paper.chapters.filter((c) => c.sections.every((s) => s.isCompleted)).length;
            return (
              <button
                key={paper.id}
                onClick={() => {
                  sound.playTick();
                  onPaperClick(paper);
                }}
                style={{
                  padding: '12px 14px',
                  borderRadius: '16px',
                  border: '0.5px solid rgba(255, 255, 255, 0.15)',
                  background: 'rgba(255, 255, 255, 0.05)',
                  color: '#fff',
                  cursor: 'pointer',
                  textAlign: 'left',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 3,
                  transition: 'all 0.2s ease'
                }}
              >
                <div style={{ fontSize: 14, fontWeight: 700, color: '#d0bcff' }}>
                  {paper.name}
                </div>
                <div style={{ fontSize: 11.5, color: 'rgba(255, 255, 255, 0.6)' }}>
                  {done} / {paper.chapters.length} Chapters Done
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
