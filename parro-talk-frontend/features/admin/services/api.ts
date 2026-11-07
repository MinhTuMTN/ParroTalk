import axios from 'axios';
import { 
  AdminVocabularyListDto, 
  AdminVocabularyRequest, 
  AdminVocabularyResponse, 
  PageResponse 
} from '../types/vocabulary';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
const ADMIN_API_BASE = `${API_BASE_URL.replace('/api/v1', '/api')}/admin/vocabularies`;

const getAuthHeaders = () => {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const adminVocabularyApi = {
  getVocabularies: async (
    page = 0, 
    size = 20, 
    search = '', 
    level = '', 
    partOfSpeech = '', 
    status = '',
    topicId = ''
  ): Promise<PageResponse<AdminVocabularyListDto>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (search) params.append('search', search);
    if (level) params.append('level', level);
    if (partOfSpeech) params.append('partOfSpeech', partOfSpeech);
    if (status) params.append('status', status);
    if (topicId) params.append('topicId', topicId);

    const res = await axios.get(`${ADMIN_API_BASE}?${params.toString()}`, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  getVocabulary: async (id: string): Promise<AdminVocabularyResponse> => {
    const res = await axios.get(`${ADMIN_API_BASE}/${id}`, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  createVocabulary: async (data: AdminVocabularyRequest): Promise<AdminVocabularyResponse> => {
    const res = await axios.post(ADMIN_API_BASE, data, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  updateVocabulary: async (id: string, data: AdminVocabularyRequest): Promise<AdminVocabularyResponse> => {
    const res = await axios.put(`${ADMIN_API_BASE}/${id}`, data, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  deleteVocabulary: async (id: string): Promise<void> => {
    const res = await axios.delete(`${ADMIN_API_BASE}/${id}`, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  aiRefineSenses: async (word: string, senses: any[]): Promise<any[]> => {
    const payload = { word, senses };
    const res = await axios.post(`${ADMIN_API_BASE}/ai/refine-senses`, payload, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  aiGenerateCommonMeaning: async (word: string): Promise<{ commonMeaningVn: string }> => {
    const payload = { word };
    const res = await axios.post(`${ADMIN_API_BASE}/ai/common-meaning`, payload, { headers: getAuthHeaders() });
    return res.data?.result !== undefined ? res.data.result : res.data;
  },

  uploadMedia: async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append("file", file);
    
    // Use the base URL but for media
    const mediaApiBase = `${API_BASE_URL.replace('/api/v1', '/api')}/admin/media/upload`;
    
    const headers = getAuthHeaders();
    const res = await axios.post(mediaApiBase, formData, { 
      headers: { ...headers, "Content-Type": "multipart/form-data" } 
    });
    return res.data?.result !== undefined ? res.data.result : res.data;
  }
};
