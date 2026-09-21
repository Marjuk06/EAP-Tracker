export type SectionKey = 'Book' | 'Slide' | 'QB' | 'Concept';

export interface StudySection {
  name: SectionKey;
  label: string;
  isCompleted: boolean;
}

export interface Chapter {
  id: string;
  name: string;
  sections: StudySection[];
}

export interface Paper {
  id: string;
  name: string;
  chapters: Chapter[];
}

export interface Subject {
  id: string;
  name: string;
  iconName: string;
  papers: Paper[];
}

export type AdmissionTrack = 'engineering' | 'medical' | 'varsity_a';

export interface RoutineItem {
  id: string;
  date: string;
  day: string;
  classSubject?: string | null;
  classRoom?: string | null;
  classTime?: string | null;
  examDetails?: string | null;
  topics: string[];
  isCompleted?: boolean;
}

export interface ExamRecord {
  id: string;
  title: string;
  examType: 'Daily' | 'Weekly' | 'Model Test' | 'Grand Final' | 'College';
  date: string;
  physicsMarks?: number;
  chemistryMarks?: number;
  mathMarks?: number;
  biologyMarks?: number;
  englishMarks?: number;
  gkMarks?: number;
  totalMarks: number;
  obtainedMarks: number;
  highestMarks?: number;
  meritPosition?: number;
  negativeMarks?: number;
  notes?: string;
}

export interface UserProfile {
  name: string;
  targetInstitution: string;
  collegeName: string;
  hscBatch: string;
  rollNo: string;
  avatarEmoji: string;
  quoteText: string;
  targetExamName: string;
  targetExamEpoch: number;
  dailyGoalHours: number;
  preferredTrack: AdmissionTrack;
  dailyRoutineAlertTime: string;
  eveningReminderTime: string;
  hapticEnabled: boolean;
  soundEnabled: boolean;
}

export interface StudySession {
  id: string;
  timestamp: number;
  durationMinutes: number;
  subject?: string;
  notes?: string;
}

export interface DayStudyLog {
  date: string; // YYYY-MM-DD
  minutes: number;
}

export interface EAPBackupSchema {
  version: number;
  exportedAt: number;
  profile: UserProfile;
  syllabusState: Record<string, boolean>; // key: "Subject_Paper_Chapter_Section"
  exams: ExamRecord[];
  routineState: Record<string, boolean>; // key: routine item id or date
  studyLogs: DayStudyLog[];
}
