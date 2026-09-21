import React, { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { ExamMarksDialog } from '../components/ExamMarksDialog';
import { ExamRecord } from '../types';
import { sound } from '../services/audio';

interface FullExamScheduleScreenProps {
  type: string;
  exams: ExamRecord[];
  onBack: () => void;
}

export const FullExamScheduleScreen: React.FC<FullExamScheduleScreenProps> = ({
  type,
  exams,
  onBack
}) => {
  const [selectedExamForMarks, setSelectedExamForMarks] = useState<{
    date: string;
    day: string;
    examName: string;
  } | null>(null);

  const allExamsList = [
    { date: '19-Aug-26', day: 'Wednesday', title: 'P-01 Introductory Exam MCQ (25)', syllabus: 'Physics Basics & Overview' },
    { date: '22-Aug-26', day: 'Saturday', title: 'P-01 MCQ (20) + Written (10)', syllabus: 'ভেক্টর (লব্ধি, উপাংশ, নদী-নৌকা)' },
    { date: '24-Aug-26', day: 'Monday', title: 'C-01 MCQ (20) + Written (10)', syllabus: 'পরিমাণগত রসায়ন (মোল, ঘনমাত্রা, টাইট্রেশন)' },
    { date: '26-Aug-26', day: 'Wednesday', title: 'M-01 MCQ (20) + Written (10)', syllabus: 'সরলরেখা (সমীকরণ, দূরত্ব, প্রতিবিম্ব)' },
    { date: '27-Aug-26', day: 'Thursday', title: 'Engg. Offline Weekly Exam-01: (P1+C1+M1)', syllabus: 'Weekly Test: P-01 + C-01 + M-01' },
    { date: '28-Aug-26', day: 'Friday', title: 'Medical Offline Weekly Exam-01', syllabus: 'Medical Weekly Test 01' },
    { date: '29-Aug-26', day: 'Saturday', title: 'M-02 MCQ (20) + Written (10)', syllabus: 'বৃত্ত (স্পর্শক, সাধারণ স্পর্শক)' },
    { date: '31-Aug-26', day: 'Monday', title: 'P-02 MCQ (20) + Written (10)', syllabus: 'গতিবিদ্যা (প্রাস, বেগ, ত্বরণ)' },
    { date: '02-Sep-26', day: 'Wednesday', title: 'C-02 MCQ (20) + Written (10)', syllabus: 'রাসায়নিক পরিবর্তন (Kp, Kc, গতিবিদ্যা)' },
    { date: '03-Sep-26', day: 'Thursday', title: 'Engg. Offline Weekly Exam-02: (P2+C2+M2)', syllabus: 'Weekly Test: P-02 + C-02 + M-02' },
    { date: '05-Sep-26', day: 'Saturday', title: 'C-03 MCQ (20) + Written (10)', syllabus: 'রাসায়নিক পরিবর্তন (অম্ল-ক্ষার, বাফার)' },
    { date: '07-Sep-26', day: 'Monday', title: 'M-03 MCQ (20) + Written (10)', syllabus: 'কণিক (পরাবৃত্ত, উপবৃত্ত, অধিবৃত্ত)' },
    { date: '09-Sep-26', day: 'Wednesday', title: 'P-03 MCQ (20) + Written (10)', syllabus: 'নিউটনীয় বলবিদ্যা (বল, ঘর্ষণ, ভরবেগ)' },
    { date: '10-Sep-26', day: 'Thursday', title: 'Engg. Offline Weekly Exam-03: (P3+C3+M3)', syllabus: 'Weekly Test: P-03 + C-03 + M-03' },
    { date: '12-Sep-26', day: 'Saturday', title: 'P-04 MCQ (20) + Written (10)', syllabus: 'নিউটনীয় বলবিদ্যা (ঘূর্ণন, ব্যাংকিং)' }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, width: '100%', paddingBottom: 60 }}>
      {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '4px 0' }}>
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
          FULL {type.toUpperCase()} EXAM SCHEDULE
        </h2>
      </div>

      {/* Exam Items Grid */}
      <div className="responsive-items-grid">
        {allExamsList.map((item, idx) => {
          const logged = exams.find((e) => e.title === item.title || (e.date === item.date && e.title.includes(item.title.slice(0, 8))));

          return (
            <div
              key={idx}
              className="haze-card"
              style={{
                padding: '18px 20px',
                borderRadius: '24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 14
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 14, flex: 1 }}>
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
                  <div style={{ fontSize: 13.5, fontWeight: 800, color: '#d0bcff' }}>{item.date.slice(0, 6)}</div>
                  <div style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.6)' }}>{item.day.slice(0, 3)}</div>
                </div>

                <div>
                  <h4 style={{ margin: 0, fontSize: 14.5, fontWeight: 700, color: '#fff' }}>
                    {item.title}
                  </h4>
                  <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.65)', marginTop: 2 }}>
                    {item.syllabus}
                  </div>
                </div>
              </div>

              <button
                onClick={() => {
                  sound.playTick();
                  setSelectedExamForMarks({
                    date: item.date,
                    day: item.day,
                    examName: item.title
                  });
                }}
                style={{
                  padding: '9px 15px',
                  borderRadius: '12px',
                  border: logged
                    ? '1px solid rgba(129, 199, 132, 0.5)'
                    : '1px solid rgba(208, 188, 255, 0.3)',
                  background: logged
                    ? 'rgba(129, 199, 132, 0.18)'
                    : 'rgba(208, 188, 255, 0.12)',
                  color: logged ? '#81c784' : '#d0bcff',
                  fontSize: 12.5,
                  fontWeight: 700,
                  cursor: 'pointer',
                  whiteSpace: 'nowrap'
                }}
              >
                {logged ? `${logged.obtainedMarks}/${logged.totalMarks}` : 'Enter Marks'}
              </button>
            </div>
          );
        })}
      </div>

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
