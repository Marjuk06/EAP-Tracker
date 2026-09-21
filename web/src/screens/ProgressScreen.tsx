import React from 'react';
import {
  TrendingUp,
  CheckCircle2,
  Calendar,
  ChevronRight,
  Zap,
  FlaskConical,
  Calculator,
  Sparkles
} from 'lucide-react';
import { Subject, ExamRecord, DayStudyLog } from '../types';
import { SquigglyProgressIndicator } from '../components/SquigglyProgressIndicator';
import { sound } from '../services/audio';

interface ProgressScreenProps {
  subjects: Subject[];
  exams: ExamRecord[];
  studyLogs: DayStudyLog[];
  onNavigateToStudyStats: () => void;
}

export const ProgressScreen: React.FC<ProgressScreenProps> = ({
  subjects,
  exams,
  studyLogs,
  onNavigateToStudyStats
}) => {
  // 1. Syllabus Metrics
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

  const syllabusPercent = totalChapters > 0 ? Math.round((completedChapters / totalChapters) * 100) : 0;

  // 2. Exam Metrics
  const totalExams = exams.length;
  const overallExamAvg = totalExams > 0
    ? Math.round(exams.reduce((acc, e) => acc + (e.obtainedMarks / e.totalMarks) * 100, 0) / totalExams)
    : 0;

  // 3. Weekly Study Time (Past 7 Days)
  const weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const logMap = new Map<string, number>();
  studyLogs.forEach((l) => logMap.set(l.date, l.minutes));

  const weekStats: { day: string; hours: number }[] = [];
  let totalWeeklyMinutes = 0;

  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const dateStr = d.toISOString().split('T')[0];
    const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
    const mins = logMap.get(dateStr) || 0;
    totalWeeklyMinutes += mins;
    weekStats.push({ day: dayName, hours: parseFloat((mins / 60).toFixed(1)) });
  }

  const totalWeeklyHours = (totalWeeklyMinutes / 60).toFixed(1);
  const maxWeeklyHours = Math.max(4, ...weekStats.map((w) => w.hours));

  const getSubjectIcon = (name: string) => {
    switch (name.toLowerCase()) {
      case 'physics':
        return <Zap size={18} color="#d0bcff" />;
      case 'chemistry':
        return <FlaskConical size={18} color="#d0bcff" />;
      case 'higher math':
        return <Calculator size={18} color="#d0bcff" />;
      case 'biology':
        return <Sparkles size={18} color="#d0bcff" />;
      default:
        return <Zap size={18} color="#d0bcff" />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, width: '100%' }}>
      {/* Title */}
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
          PROGRESS
        </h1>
      </div>

      {/* Top 2 Analytics Cards (Side-by-Side on Desktop) */}
      <div className="responsive-two-col">
        {/* 1. Performance Analytics Interactive Graph Card */}
        <div
          className="haze-card"
          style={{
            padding: '22px 20px',
            borderRadius: '26px',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between'
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div
                  style={{
                    width: 38,
                    height: 38,
                    borderRadius: '50%',
                    background: 'rgba(208, 188, 255, 0.18)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#d0bcff'
                  }}
                >
                  <TrendingUp size={20} />
                </div>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 800, color: '#d0bcff', letterSpacing: '1px' }}>
                    PERFORMANCE GRAPH
                  </div>
                  <div style={{ fontSize: 13.5, fontWeight: 700, color: '#fff' }}>
                    Exam Score Trajectory
                  </div>
                </div>
              </div>

              <div
                style={{
                  fontSize: 14.5,
                  fontWeight: 800,
                  color: '#81c784',
                  fontFamily: 'var(--font-mono)'
                }}
              >
                {overallExamAvg > 0 ? `${overallExamAvg}% Avg` : 'No Scores'}
              </div>
            </div>
          </div>

          {/* Score Bar / Line Visualizer */}
          <div style={{ height: 110, display: 'flex', alignItems: 'flex-end', gap: 8, paddingTop: 10 }}>
            {exams.length === 0 ? (
              <div style={{ flex: 1, textAlign: 'center', color: 'rgba(255,255,255,0.4)', fontSize: 13, alignSelf: 'center' }}>
                Log exam scores to visualize your performance trajectory
              </div>
            ) : (
              exams.slice(0, 8).reverse().map((ex) => {
                const p = Math.round((ex.obtainedMarks / ex.totalMarks) * 100);
                return (
                  <div
                    key={ex.id}
                    style={{
                      flex: 1,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 6,
                      height: '100%',
                      justifyContent: 'flex-end'
                    }}
                  >
                    <span style={{ fontSize: 9.5, fontWeight: 700, color: '#d0bcff' }}>{p}%</span>
                    <div
                      style={{
                        width: '100%',
                        maxWidth: 28,
                        height: `${Math.max(10, p)}%`,
                        borderRadius: '6px 6px 0 0',
                        background: p >= 80 ? '#81c784' : '#d0bcff',
                        boxShadow: '0 0 10px rgba(208, 188, 255, 0.25)'
                      }}
                    />
                    <span style={{ fontSize: 9.5, color: 'rgba(255,255,255,0.55)', whiteSpace: 'nowrap' }}>
                      {ex.date.slice(0, 5)}
                    </span>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* 2. Weekly Study Time & Consistency Analytics Card */}
        <div
          className="haze-card"
          onClick={() => {
            sound.playTick();
            onNavigateToStudyStats();
          }}
          style={{
            padding: '22px 20px',
            borderRadius: '26px',
            cursor: 'pointer',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between'
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div
                  style={{
                    width: 38,
                    height: 38,
                    borderRadius: '50%',
                    background: 'rgba(208, 188, 255, 0.18)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#d0bcff'
                  }}
                >
                  <Calendar size={18} />
                </div>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 800, color: '#d0bcff', letterSpacing: '1px' }}>
                    WEEKLY STUDY TIME
                  </div>
                  <div style={{ fontSize: 13.5, fontWeight: 700, color: '#fff' }}>
                    {totalWeeklyHours}h Total This Week
                  </div>
                </div>
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  padding: '5px 12px',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(208, 188, 255, 0.14)',
                  color: '#d0bcff',
                  fontSize: 12,
                  fontWeight: 700
                }}
              >
                <span>Full Stats</span>
                <ChevronRight size={14} />
              </div>
            </div>
          </div>

          {/* 7-Day Consistency Bars */}
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 110, paddingTop: 10 }}>
            {weekStats.map((item, idx) => {
              const h = item.hours;
              const heightPercent = Math.max(8, Math.round((h / maxWeeklyHours) * 100));

              return (
                <div
                  key={idx}
                  style={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: 6,
                    height: '100%',
                    justifyContent: 'flex-end'
                  }}
                >
                  {h > 0 && <span style={{ fontSize: 9.5, fontWeight: 700, color: '#d0bcff' }}>{h}h</span>}
                  <div
                    style={{
                      width: '100%',
                      maxWidth: 24,
                      height: `${heightPercent}%`,
                      borderRadius: '4px 4px 0 0',
                      background: h > 0 ? '#d0bcff' : 'rgba(255, 255, 255, 0.08)'
                    }}
                  />
                  <span style={{ fontSize: 10.5, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>
                    {item.day}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 3. Overview Metric Cards Row (Syllabus & Exam Average) */}
      <div className="responsive-two-col">
        {/* Syllabus Mastery */}
        <div
          className="haze-card"
          style={{
            padding: '18px 20px',
            borderRadius: '20px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#d0bcff' }}>
            <CheckCircle2 size={18} />
            <span style={{ fontSize: 13, fontWeight: 700 }}>Syllabus Mastery</span>
          </div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 28, fontWeight: 800, color: '#fff', margin: '8px 0 4px 0' }}>
            {syllabusPercent}%
          </div>
          <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)' }}>
            {completedChapters} / {totalChapters} Chapters Completed
          </div>
        </div>

        {/* Exam Average */}
        <div
          className="haze-card"
          style={{
            padding: '18px 20px',
            borderRadius: '20px'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#81c784' }}>
            <TrendingUp size={18} />
            <span style={{ fontSize: 13, fontWeight: 700 }}>Exam Average Score</span>
          </div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 28, fontWeight: 800, color: '#fff', margin: '8px 0 4px 0' }}>
            {totalExams > 0 ? `${overallExamAvg}%` : 'N/A'}
          </div>
          <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)' }}>
            {totalExams > 0 ? `${totalExams} Exams Logged` : 'No scores logged yet'}
          </div>
        </div>
      </div>

      {/* 4. Subject Mastery Breakdown List in Responsive 2-Col Grid */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h3 style={{ margin: '6px 0 2px 0', fontSize: 17, fontWeight: 800, color: '#fff', letterSpacing: '0.5px' }}>
          SUBJECT MASTERY BREAKDOWN
        </h3>

        <div className="responsive-two-col">
          {subjects.map((sub) => {
            let subTotal = 0;
            let subDone = 0;

            sub.papers.forEach((p) => {
              p.chapters.forEach((c) => {
                subTotal++;
                if (c.sections.every((sec) => sec.isCompleted)) {
                  subDone++;
                }
              });
            });

            const pRatio = subTotal > 0 ? subDone / subTotal : 0;
            const pInt = Math.round(pRatio * 100);

            return (
              <div
                key={sub.id}
                className="haze-card"
                style={{
                  padding: '16px 20px',
                  borderRadius: '20px'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: 10,
                        background: 'rgba(208, 188, 255, 0.15)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      {getSubjectIcon(sub.name)}
                    </div>
                    <span style={{ fontSize: 15, fontWeight: 700, color: '#fff' }}>{sub.name}</span>
                  </div>

                  <span style={{ fontSize: 14.5, fontWeight: 800, color: '#d0bcff' }}>
                    {pInt}%
                  </span>
                </div>

                <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)', marginBottom: 8 }}>
                  {subDone} / {subTotal} Chapters Completed
                </div>

                <SquigglyProgressIndicator
                  progress={pRatio}
                  color="#d0bcff"
                  trackColor="rgba(255, 255, 255, 0.1)"
                />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
