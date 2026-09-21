import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, FileText, ChevronDown } from 'lucide-react';
import { ExamRecord } from '../types';
import { ExamMarksDialog } from '../components/ExamMarksDialog';
import { sound } from '../services/audio';
import { storage } from '../services/storage';

interface ExamsScreenProps {
  exams: ExamRecord[];
  onNavigateToFullExams: (type: string) => void;
}

export const ExamsScreen: React.FC<ExamsScreenProps> = ({
  exams,
  onNavigateToFullExams
}) => {
  const [examRoutineType, setExamRoutineType] = useState<'Offline' | 'Online'>('Offline');
  const [currentWeekPage, setCurrentWeekPage] = useState<number>(0);
  const [showSyllabus, setShowSyllabus] = useState<boolean>(false);
  const [isFullExamsExpanded, setIsFullExamsExpanded] = useState<boolean>(false);

  const [selectedExamForMarks, setSelectedExamForMarks] = useState<{
    date: string;
    day: string;
    examName: string;
  } | null>(null);

  const offlineExamList = [
    // Week 1
    { date: '15-Aug-26', day: 'Saturday', exams: [] as string[], syllabus: ['কোর্স শুরুর পূর্বপ্রস্তুতি'] },
    { date: '16-Aug-26', day: 'Sunday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্রস্তুতি'] },
    { date: '17-Aug-26', day: 'Monday', exams: [] as string[], syllabus: ['অরিয়েন্টেশন ও দিকনির্দেশনা'] },
    { date: '18-Aug-26', day: 'Tuesday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্রস্তুতি'] },
    { date: '19-Aug-26', day: 'Wednesday', exams: ['P-01 Introductory Exam MCQ (25)'], syllabus: ['Introductory Exam: Physics Basics & Overview'] },
    { date: '20-Aug-26', day: 'Thursday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্র্যাকটিস'] },
    { date: '21-Aug-26', day: 'Friday', exams: [] as string[], syllabus: ['সাপ্তাহিক রিভিশন'] },
    // Week 2
    { date: '22-Aug-26', day: 'Saturday', exams: ['P-01 MCQ (20) + Written (10)'], syllabus: ['P-01: ভেক্টর (ভেক্টর লব্ধি, উপাংশ বিভাজন, ডট ও ক্রস গুণন, ভেক্টর ক্যালকুলাস, নদী-নৌকা)'] },
    { date: '23-Aug-26', day: 'Sunday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্র্যাকটিস'] },
    { date: '24-Aug-26', day: 'Monday', exams: ['C-01 MCQ (20) + Written (10)'], syllabus: ['C-01: পরিমাণগত রসায়ন (মোল, ঘনমাত্রা, জারণ-বিজারণ, টাইট্রেশন) ও ল্যাবরেটরি'] },
    { date: '25-Aug-26', day: 'Tuesday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্র্যাকটিস'] },
    { date: '26-Aug-26', day: 'Wednesday', exams: ['M-01 MCQ (20) + Written (10)'], syllabus: ['M-01: সরলরেখা (স্থানাঙ্ক ব্যবস্থা, বিভিন্ন ক্ষেত্রে সরলরেখার সমীকরণ ও প্রতিবিম্ব)'] },
    { date: '27-Aug-26', day: 'Thursday', exams: ['Engg. Offline Weekly Exam-01: (P1+C1+M1)'], syllabus: ['Weekly Test: P-01 + C-01 + M-01'] },
    { date: '28-Aug-26', day: 'Friday', exams: ['Medical Offline Weekly Exam-01'], syllabus: ['Medical Weekly Test 01'] },
    // Week 3
    { date: '29-Aug-26', day: 'Saturday', exams: ['M-02 MCQ (20) + Written (10)'], syllabus: ['M-02: বৃত্ত (বৃত্তের সমীকরণ, স্পর্শক-ছেদক, দুটি বৃত্তের পারস্পরিক অবস্থান)'] },
    { date: '30-Aug-26', day: 'Sunday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্র্যাকটিস'] },
    { date: '31-Aug-26', day: 'Monday', exams: ['P-02 MCQ (20) + Written (10)'], syllabus: ['P-02: গতিবিদ্যা (প্রাস, বেগ, ত্বরণ, লেখচিত্র)'] },
    { date: '01-Sep-26', day: 'Tuesday', exams: [] as string[], syllabus: ['স্ব-অধ্যয়ন ও প্র্যাকটিস'] },
    { date: '02-Sep-26', day: 'Wednesday', exams: ['C-02 MCQ (20) + Written (10)'], syllabus: ['C-02: রাসায়নিক পরিবর্তন (Kp, Kc, লা-শাতেলিয়ার নীতি, গতিবিদ্যা)'] },
    { date: '03-Sep-26', day: 'Thursday', exams: ['Engg. Offline Weekly Exam-02: (P2+C2+M2)'], syllabus: ['Weekly Test: P-02 + C-02 + M-02'] },
    { date: '04-Sep-26', day: 'Friday', exams: ['Medical Offline Weekly Exam-02'], syllabus: ['Medical Weekly Test 02'] }
  ];

  const onlineExamList = [
    { date: '19-Aug-26', day: 'Wednesday', exams: ['P-01 Introductory Online Exam'], syllabus: ['Physics Online Introduction'] },
    { date: '22-Aug-26', day: 'Saturday', exams: ['P-01 Online MCQ + Written'], syllabus: ['ভেক্টর অনলাইন মূল্যায়ন'] },
    { date: '24-Aug-26', day: 'Monday', exams: ['C-01 Online MCQ + Written'], syllabus: ['পরিমাণগত রসায়ন অনলাইন মূল্যায়ন'] },
    { date: '26-Aug-26', day: 'Wednesday', exams: ['M-01 Online MCQ + Written'], syllabus: ['সরলরেখা অনলাইন মূল্যায়ন'] },
    { date: '27-Aug-26', day: 'Thursday', exams: ['Engg. Online Weekly Exam-01'], syllabus: ['Weekly Online Test 01'] },
    { date: '29-Aug-26', day: 'Saturday', exams: ['M-02 Online MCQ + Written'], syllabus: ['বৃত্ত অনলাইন মূল্যায়ন'] },
    { date: '31-Aug-26', day: 'Monday', exams: ['P-02 Online MCQ + Written'], syllabus: ['গতিবিদ্যা অনলাইন মূল্যায়ন'] }
  ];

  const currentExamData = examRoutineType === 'Offline' ? offlineExamList : onlineExamList;
  const pageSize = 7;
  const totalPages = Math.ceil(currentExamData.length / pageSize);
  const currentWeekExams = currentExamData.slice(currentWeekPage * pageSize, (currentWeekPage + 1) * pageSize);

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
      {/* Title */}
      <div style={{ textAlign: 'center' }}>
        <h1
          style={{
            margin: 0,
            fontSize: 26,
            fontWeight: 900,
            letterSpacing: '2px',
            color: '#fff'
          }}
        >
          EXAMS
        </h1>
      </div>

      {/* Offline / Online Connected Toggle Pill (ExamsScreen.kt) */}
      <div
        style={{
          display: 'flex',
          height: 56,
          gap: 4,
          width: '100%'
        }}
      >
        {(['Offline', 'Online'] as const).map((type) => {
          const isSelected = examRoutineType === type;
          return (
            <div
              key={type}
              onClick={() => {
                sound.playTick();
                setExamRoutineType(type);
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

      {/* Exam Pager Card (Frosted Glass Effect from ExamsScreen.kt) */}
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
        {/* Header with WEEK X and Syllabus Toggle Pill */}
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

            {/* Syllabus Toggle Pill */}
            <div
              onClick={() => {
                sound.playTick();
                setShowSyllabus((prev) => !prev);
              }}
              style={{
                borderRadius: showSyllabus ? '24px' : '4px',
                background: showSyllabus ? '#d0bcff' : 'rgba(255, 255, 255, 0.2)',
                color: showSyllabus ? '#381e72' : '#fff',
                padding: '4px 14px',
                fontSize: 13,
                fontWeight: 800,
                cursor: 'pointer',
                transition: 'all 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
              }}
            >
              Syllabus
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

        {/* Exam Rows (Exact ExamRow from ExamsScreen.kt) */}
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          {currentWeekExams.map((item, idx) => {
            const dateParts = item.date.split('-');
            const dayNum = dateParts[0];
            const monthName = dateParts[1] || 'Aug';
            const dayAbbr = item.day.slice(0, 3);

            return (
              <div key={idx}>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    padding: '12px 0'
                  }}
                >
                  {/* Left Column: Date & Day */}
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

                  {/* Right Column: Exams or Syllabus */}
                  <div style={{ flex: 1, paddingLeft: 12 }}>
                    {showSyllabus ? (
                      // Show Syllabus Details
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {item.exams.map((examName, eIdx) => (
                          <div key={eIdx} style={{ fontSize: 13.5, fontWeight: 700, color: '#d0bcff' }}>
                            {examName}
                          </div>
                        ))}
                        {item.syllabus.map((syl, sIdx) => (
                          <div
                            key={sIdx}
                            style={{
                              fontSize: 12.5,
                              color: 'rgba(255, 255, 255, 0.92)',
                              lineHeight: '17px'
                            }}
                          >
                            {syl}
                          </div>
                        ))}
                      </div>
                    ) : (
                      // Normal View: Clickable Exam Pills with score badge
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                        {item.exams.length > 0 ? (
                          item.exams.map((examName, eIdx) => {
                            const score = getScoreInfo(item.date, examName);
                            const badgeColor = score ? getScoreBadgeColor(score.percentage) : null;

                            return (
                              <div
                                key={eIdx}
                                onClick={() => {
                                  sound.playTick();
                                  setSelectedExamForMarks({
                                    date: item.date,
                                    day: item.day,
                                    examName: examName
                                  });
                                }}
                                style={{
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'space-between',
                                  padding: score ? '4px 8px' : '2px 0',
                                  borderRadius: '10px',
                                  background: score ? 'rgba(255, 255, 255, 0.08)' : 'transparent',
                                  cursor: 'pointer'
                                }}
                              >
                                <span
                                  style={{
                                    fontSize: 13,
                                    fontWeight: 600,
                                    color: '#d0bcff',
                                    lineHeight: '17px',
                                    flex: 1
                                  }}
                                >
                                  {examName}
                                </span>

                                {score && badgeColor ? (
                                  <span
                                    style={{
                                      fontSize: 11,
                                      fontWeight: 800,
                                      padding: '2px 8px',
                                      borderRadius: '6px',
                                      background: badgeColor.bg,
                                      color: badgeColor.text,
                                      marginLeft: 8,
                                      whiteSpace: 'nowrap'
                                    }}
                                  >
                                    {score.obtained}/{score.max}
                                  </span>
                                ) : (
                                  <span
                                    style={{
                                      fontSize: 11,
                                      fontWeight: 700,
                                      padding: '2px 8px',
                                      borderRadius: '6px',
                                      background: 'rgba(208, 188, 255, 0.15)',
                                      color: '#d0bcff',
                                      marginLeft: 8,
                                      whiteSpace: 'nowrap'
                                    }}
                                  >
                                    Enter Marks
                                  </span>
                                )}
                              </div>
                            );
                          })
                        ) : (
                          <div style={{ fontSize: 13, color: 'rgba(255, 255, 255, 0.4)' }}>
                            No exam scheduled
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Horizontal Divider */}
                {idx < currentWeekExams.length - 1 && (
                  <div style={{ height: '0.5px', background: 'rgba(255, 255, 255, 0.15)', margin: '4px 0' }} />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Full Exam Schedule Split Button (ExamsScreen.kt) */}
      <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
        <div
          onClick={() => {
            sound.playTick();
            setIsFullExamsExpanded((prev) => !prev);
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
              FULL EXAM SCHEDULE
            </span>
          </div>

          {/* Right Chevron Box */}
          <div
            style={{
              width: 56,
              height: '100%',
              borderRadius: isFullExamsExpanded ? '4px 28px 28px 4px' : '4px 28px 28px 4px',
              background: 'rgba(204, 194, 220, 0.18)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#e8def8'
            }}
          >
            <div
              style={{
                transform: isFullExamsExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                transition: 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
              }}
            >
              <ChevronDown size={22} />
            </div>
          </div>
        </div>

        {/* Expanded OFFLINE / ONLINE Options (ExamsScreen.kt) */}
        {isFullExamsExpanded && (
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
                onNavigateToFullExams('offline');
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
                onNavigateToFullExams('online');
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
