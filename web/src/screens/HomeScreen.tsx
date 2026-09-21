import React, { useState } from 'react';
import { DailyStudyGoalCard } from '../components/DailyStudyGoalCard';
import { ExamMarksDialog } from '../components/ExamMarksDialog';
import { RoutineItem } from '../types';
import { sound } from '../services/audio';
import { storage } from '../services/storage';
import { FileText, ChevronDown, ChevronLeft, ChevronRight, Bell, Settings as SettingsIcon, X, Sparkles } from 'lucide-react';

interface HomeScreenProps {
  goalHours: number;
  todayMinutes: number;
  routines: RoutineItem[];
  onNavigateToFullRoutine: (type: string) => void;
  onNavigateToStudyStats: () => void;
  onOpenFullScreenTimer: () => void;
  onNavigateToSettings?: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  goalHours,
  todayMinutes,
  routines,
  onNavigateToFullRoutine,
  onNavigateToStudyStats,
  onOpenFullScreenTimer,
  onNavigateToSettings
}) => {
  const [routineType, setRoutineType] = useState<'Offline' | 'Online'>('Offline');
  const [currentWeekPage, setCurrentWeekPage] = useState<number>(0);
  const [showTopics, setShowTopics] = useState<boolean>(false);
  const [isFullRoutineExpanded, setIsFullRoutineExpanded] = useState<boolean>(false);
  const [showInboxModal, setShowInboxModal] = useState<boolean>(false);

  const [selectedExamForMarks, setSelectedExamForMarks] = useState<{
    date: string;
    day: string;
    examName: string;
  } | null>(null);

  const pageSize = 7;
  const totalPages = Math.ceil(routines.length / pageSize);
  const currentWeekDays = routines.slice(currentWeekPage * pageSize, (currentWeekPage + 1) * pageSize);

  const loggedExams = storage.getExams();

  const getScoreInfo = (date: string, examName: string) => {
    const found = loggedExams.find(
      (e) => e.title === examName || (e.date === date && e.title.includes(examName.slice(0, 8)))
    );
    if (!found) return null;
    const percentage = found.totalMarks > 0 ? (found.obtainedMarks / found.totalMarks) * 100 : 0;
    return {
      obtained: found.obtainedMarks,
      max: found.totalMarks,
      percentage
    };
  };

  const getScoreBadgeColor = (percentage: number) => {
    if (percentage >= 80) return { bg: 'rgba(76, 175, 80, 0.2)', text: '#81c784' };
    if (percentage >= 60) return { bg: 'rgba(33, 150, 243, 0.2)', text: '#64b5f6' };
    if (percentage >= 40) return { bg: 'rgba(255, 152, 0, 0.2)', text: '#ffb74d' };
    return { bg: 'rgba(244, 67, 54, 0.2)', text: '#e57373' };
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, width: '100%', maxWidth: 960, margin: '0 auto' }}>
      {/* Top Header Row with Title and Top-Right Action Icons (Bell + Gear) */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 4px' }}>
        <h1
          style={{
            margin: 0,
            fontSize: 26,
            fontWeight: 900,
            letterSpacing: '2px',
            color: '#fff'
          }}
        >
          HOME
        </h1>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {/* 1. Notification (Bell) Icon beside Settings Gear */}
          <button
            onClick={() => {
              sound.playTick();
              setShowInboxModal(true);
            }}
            title="Notification Inbox"
            style={{
              width: 42,
              height: 42,
              borderRadius: '50%',
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              position: 'relative'
            }}
          >
            <Bell size={18} />
          </button>

          {/* 2. Settings (Gear) Icon */}
          <button
            onClick={() => {
              sound.playTick();
              if (onNavigateToSettings) onNavigateToSettings();
            }}
            title="Profile & Settings"
            style={{
              width: 42,
              height: 42,
              borderRadius: '50%',
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer'
            }}
          >
            <SettingsIcon size={18} />
          </button>
        </div>
      </div>

      {/* Daily Study Goal Card */}
      <DailyStudyGoalCard
        goalHours={goalHours}
        todayMinutes={todayMinutes}
        onNavigateToStats={onNavigateToStudyStats}
        onOpenFullScreenTimer={onOpenFullScreenTimer}
      />

      {/* Offline / Online Connected Toggle (Exact HomeScreen.kt) */}
      <div
        style={{
          display: 'flex',
          height: 56,
          gap: 4,
          width: '100%'
        }}
      >
        {(['Offline', 'Online'] as const).map((type) => {
          const isSelected = routineType === type;
          return (
            <div
              key={type}
              onClick={() => {
                sound.playTick();
                setRoutineType(type);
                setCurrentWeekPage(0);
              }}
              style={{
                flex: isSelected ? 1.5 : 1,
                height: '100%',
                borderRadius: isSelected ? '28px' : '12px',
                background: isSelected ? '#d0bcff' : 'rgba(73, 69, 79, 0.5)',
                color: isSelected ? '#381e72' : 'rgba(255, 255, 255, 0.7)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                fontWeight: 800,
                fontSize: 14,
                letterSpacing: '1px',
                transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)'
              }}
            >
              {type.toUpperCase()}
            </div>
          );
        })}
      </div>

      {/* Routine Pager Card (Exact Frosted Glass Effect from HomeScreen.kt) */}
      <div
        className="haze-card"
        style={{
          borderRadius: '28px',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: 16
        }}
      >
        {/* Header with WEEK X and Topics Toggle Pill */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <button
              onClick={() => {
                sound.playTick();
                setCurrentWeekPage((prev) => Math.max(0, prev - 1));
              }}
              disabled={currentWeekPage === 0}
              style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                background: 'rgba(255, 255, 255, 0.06)',
                color: currentWeekPage === 0 ? 'rgba(255,255,255,0.2)' : '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: currentWeekPage === 0 ? 'default' : 'pointer'
              }}
            >
              <ChevronLeft size={18} />
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span
              style={{
                fontSize: 16,
                fontWeight: 800,
                color: '#fff',
                letterSpacing: '1px'
              }}
            >
              WEEK {currentWeekPage + 1}
            </span>

            {/* Animated Topics Toggle Pill (HomeScreen.kt) */}
            <div
              onClick={() => {
                sound.playTick();
                setShowTopics((prev) => !prev);
              }}
              style={{
                borderRadius: showTopics ? '24px' : '4px',
                background: showTopics ? '#d0bcff' : 'rgba(255, 255, 255, 0.2)',
                color: showTopics ? '#381e72' : '#fff',
                padding: '4px 14px',
                fontSize: 13,
                fontWeight: 800,
                cursor: 'pointer',
                transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
              }}
            >
              Topics
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <button
              onClick={() => {
                sound.playTick();
                setCurrentWeekPage((prev) => Math.min(totalPages - 1, prev + 1));
              }}
              disabled={currentWeekPage >= totalPages - 1}
              style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                background: 'rgba(255, 255, 255, 0.06)',
                color: currentWeekPage >= totalPages - 1 ? 'rgba(255,255,255,0.2)' : '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: currentWeekPage >= totalPages - 1 ? 'default' : 'pointer'
              }}
            >
              <ChevronRight size={18} />
            </button>
          </div>
        </div>

        {/* Days Rows (Exact RoutineRow from HomeScreen.kt) */}
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          {currentWeekDays.map((item, idx) => {
            const dateParts = item.date.split('-');
            const dayNum = dateParts[0];
            const monthName = dateParts[1] || 'Aug';
            const dayAbbr = item.day.slice(0, 3);

            const score = item.examDetails ? getScoreInfo(item.date, item.examDetails) : null;
            const badgeColor = score ? getScoreBadgeColor(score.percentage) : null;

            return (
              <div key={item.id || idx}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    padding: '12px 0'
                  }}
                >
                  {/* Left Column: Date & Day (Exact HomeScreen.kt) */}
                  <div style={{ width: 78, flexShrink: 0 }}>
                    <div
                      style={{
                        fontSize: 20,
                        fontWeight: 900,
                        color: '#fff',
                        lineHeight: 1.1
                      }}
                    >
                      {dayNum}
                    </div>
                    <div
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        color: 'rgba(255, 255, 255, 0.7)',
                        marginTop: 2
                      }}
                    >
                      {monthName} . {dayAbbr}
                    </div>
                  </div>

                  {/* Right Column: Content with crossfade for Topics */}
                  <div style={{ flex: 1, paddingLeft: 12 }}>
                    {showTopics ? (
                      // Show Topics View
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {item.classSubject && (
                          <div style={{ fontSize: 13.5, fontWeight: 700, color: '#d0bcff' }}>
                            {item.classSubject}
                          </div>
                        )}
                        {item.topics && item.topics.length > 0 ? (
                          item.topics.map((topic, tIdx) => (
                            <div
                              key={tIdx}
                              style={{
                                fontSize: 12.5,
                                color: 'rgba(255, 255, 255, 0.92)',
                                lineHeight: '17px'
                              }}
                            >
                              {topic}
                            </div>
                          ))
                        ) : item.examDetails ? (
                          <div style={{ fontSize: 12.5, color: '#ccc2dc', lineHeight: '16px' }}>
                            {item.examDetails}
                          </div>
                        ) : null}
                      </div>
                    ) : (
                      // Normal View (Subject + Clickable Exam Pill)
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {item.classSubject && (
                          <div style={{ fontSize: 13.5, fontWeight: 700, color: '#fff' }}>
                            {item.classSubject}
                          </div>
                        )}

                        {item.examDetails ? (
                          <div
                            onClick={() => {
                              sound.playTick();
                              setSelectedExamForMarks({
                                date: item.date,
                                day: item.day,
                                examName: item.examDetails || 'Daily Exam'
                              });
                            }}
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                              padding: score ? '3px 6px' : '2px 0',
                              borderRadius: '10px',
                              background: score ? 'rgba(255, 255, 255, 0.08)' : 'transparent',
                              cursor: 'pointer'
                            }}
                          >
                            <span
                              style={{
                                fontSize: 12.5,
                                fontWeight: 600,
                                color: '#d0bcff',
                                lineHeight: '16px',
                                flex: 1
                              }}
                            >
                              {item.examDetails}
                            </span>

                            {score && badgeColor && (
                              <span
                                style={{
                                  fontSize: 10.5,
                                  fontWeight: 800,
                                  padding: '2px 6px',
                                  borderRadius: '6px',
                                  background: badgeColor.bg,
                                  color: badgeColor.text,
                                  marginLeft: 6,
                                  whiteSpace: 'nowrap'
                                }}
                              >
                                {score.obtained}/{score.max}
                              </span>
                            )}
                          </div>
                        ) : null}

                        {!item.classSubject && !item.examDetails && (
                          <div style={{ fontSize: 13, color: 'rgba(255, 255, 255, 0.4)' }}>
                            Self Study & Prep
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Horizontal Divider */}
                {idx < currentWeekDays.length - 1 && (
                  <div style={{ height: '0.5px', background: 'rgba(255, 255, 255, 0.15)', margin: '4px 0' }} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Full Routine Split Button (Expandable from HomeScreen.kt) */}
      <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
        <div
          onClick={() => {
            sound.playTick();
            setIsFullRoutineExpanded((prev) => !prev);
          }}
          style={{
            display: 'flex',
            height: 56,
            alignItems: 'center',
            cursor: 'pointer',
            gap: 2
          }}
        >
          {/* Left Box */}
          <div
            style={{
              flex: 1,
              height: '100%',
              borderRadius: '28px 4px 4px 28px',
              background: 'rgba(204, 194, 220, 0.18)',
              color: '#e8def8',
              display: 'flex',
              alignItems: 'center',
              padding: '0 24px',
              gap: 12
            }}
          >
            <FileText size={20} color="#e8def8" />
            <span style={{ fontSize: 14, fontWeight: 800, letterSpacing: '1px' }}>
              FULL ROUTINE
            </span>
          </div>

          {/* Right Chevron Box */}
          <div
            style={{
              width: 56,
              height: '100%',
              borderRadius: isFullRoutineExpanded ? '4px 28px 28px 4px' : '4px 28px 28px 4px',
              background: 'rgba(204, 194, 220, 0.18)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#e8def8'
            }}
          >
            <div
              style={{
                transform: isFullRoutineExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
              }}
            >
              <ChevronDown size={22} />
            </div>
          </div>
        </div>

        {/* Expanded OFFLINE / ONLINE Options (HomeScreen.kt) */}
        {isFullRoutineExpanded && (
          <div
            style={{
              display: 'flex',
              height: 48,
              gap: 2,
              marginTop: 12,
              width: '100%'
            }}
          >
            <div
              onClick={() => {
                sound.playTick();
                onNavigateToFullRoutine('offline');
              }}
              style={{
                flex: 1,
                height: '100%',
                borderRadius: '24px 4px 4px 24px',
                background: 'rgba(73, 69, 79, 0.7)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 13.5,
                fontWeight: 800,
                cursor: 'pointer'
              }}
            >
              OFFLINE
            </div>

            <div
              onClick={() => {
                sound.playTick();
                onNavigateToFullRoutine('online');
              }}
              style={{
                flex: 1,
                height: '100%',
                borderRadius: '4px 24px 24px 4px',
                background: 'rgba(73, 69, 79, 0.7)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 13.5,
                fontWeight: 800,
                cursor: 'pointer'
              }}
            >
              ONLINE
            </div>
          </div>
        )}
      </div>

      {/* Exam Marks Dialog */}
      {selectedExamForMarks && (
        <ExamMarksDialog
          date={selectedExamForMarks.date}
          day={selectedExamForMarks.day}
          examName={selectedExamForMarks.examName}
          onDismiss={() => setSelectedExamForMarks(null)}
        />
      )}
    </div>
  );
};
