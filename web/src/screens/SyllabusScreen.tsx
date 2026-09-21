import React, { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Subject, Paper } from '../types';
import { OverallProgress } from '../components/OverallProgress';
import { SyllabusSplitButton } from '../components/SyllabusSplitButton';
import { ChapterExpandableItem } from '../components/ChapterExpandableItem';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface SyllabusScreenProps {
  subjects: Subject[];
}

export const SyllabusScreen: React.FC<SyllabusScreenProps> = ({ subjects }) => {
  const [expandedSubjectName, setExpandedSubjectName] = useState<string | null>(null);
  const [selectedSubjectName, setSelectedSubjectName] = useState<string | null>(null);
  const [selectedPaperName, setSelectedPaperName] = useState<string | null>(null);
  const [expandedChapterIndex, setExpandedChapterIndex] = useState<number | null>(null);

  // Total completed vs total chapters
  let totalChapters = 0;
  let completedChapters = 0;

  subjects.forEach((s) => {
    s.papers.forEach((p) => {
      p.chapters.forEach((c) => {
        totalChapters++;
        if (c.sections.every((sec) => sec.isCompleted)) {
          completedChapters++;
        }
      });
    });
  });

  const handleToggleSubject = (name: string) => {
    setExpandedSubjectName((prev) => (prev === name ? null : name));
  };

  const handlePaperClick = (sub: Subject, paper: Paper) => {
    setSelectedSubjectName(sub.name);
    setSelectedPaperName(paper.name);
    setExpandedChapterIndex(null);
  };

  const handleBackToSubjectList = () => {
    sound.playTick();
    setSelectedPaperName(null);
  };

  const handleSectionToggle = (chapterIndex: number, sectionIndex: number) => {
    if (!selectedSubjectName || !selectedPaperName) return;

    const sub = subjects.find((s) => s.name === selectedSubjectName);
    const paper = sub?.papers.find((p) => p.name === selectedPaperName);
    const chap = paper?.chapters[chapterIndex];
    const sec = chap?.sections[sectionIndex];

    if (!chap || !sec) return;

    const nextStatus = !sec.isCompleted;
    storage.saveSyllabusSection(
      selectedSubjectName,
      selectedPaperName,
      chap.name,
      sec.name,
      nextStatus
    );

    if (nextStatus) {
      sound.playSuccess();
    } else {
      sound.playTick();
    }
  };

  const currentPaper = subjects
    .find((s) => s.name === selectedSubjectName)
    ?.papers.find((p) => p.name === selectedPaperName);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, width: '100%' }}>
      {!selectedPaperName ? (
        // VIEW 1: SUBJECTS LIST
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div style={{ textAlign: 'center', marginBottom: 2 }}>
            <h1
              style={{
                margin: 0,
                fontSize: 26,
                fontWeight: 900,
                letterSpacing: '2px',
                color: '#fff'
              }}
            >
              SYLLABUS
            </h1>
          </div>

          <OverallProgress
            completedChapters={completedChapters}
            totalChapters={totalChapters}
          />

          {/* Subjects Grid (1-column on mobile, 2-column on desktop) */}
          <div className="responsive-two-col">
            {subjects.map((sub) => (
              <SyllabusSplitButton
                key={sub.id}
                subject={sub}
                isExpanded={expandedSubjectName === sub.name}
                onToggle={() => handleToggleSubject(sub.name)}
                onPaperClick={(paper) => handlePaperClick(sub, paper)}
              />
            ))}
          </div>
        </div>
      ) : (
        // VIEW 2: CHAPTER LIST
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Header Row with Back Arrow */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '4px 0' }}>
            <button
              onClick={handleBackToSubjectList}
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

            <div>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#d0bcff', letterSpacing: '1px' }}>
                {selectedSubjectName?.toUpperCase()}
              </div>
              <h2 style={{ margin: 0, fontSize: 22, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
                {selectedPaperName?.toUpperCase()}
              </h2>
            </div>
          </div>

          <div style={{ height: 1, background: 'rgba(255, 255, 255, 0.12)', margin: '2px 0 8px 0' }} />

          {/* Chapters Responsive Grid */}
          <div className="responsive-two-col">
            {currentPaper?.chapters.map((chapter, chapIdx) => (
              <ChapterExpandableItem
                key={chapter.id}
                chapter={chapter}
                isExpanded={expandedChapterIndex === chapIdx}
                onToggle={() => setExpandedChapterIndex((prev) => (prev === chapIdx ? null : chapIdx))}
                onSectionToggle={(secIdx) => handleSectionToggle(chapIdx, secIdx)}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
