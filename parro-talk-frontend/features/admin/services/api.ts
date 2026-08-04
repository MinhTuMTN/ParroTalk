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
    status = ''
  ): Promise<PageResponse<AdminVocabularyListDto>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (search) params.append('search', search);
    if (level) params.append('level', level);
    if (partOfSpeech) params.append('partOfSpeech', partOfSpeech);
    if (status) params.append('status', status);

    const res = await axios.get(`${ADMIN_API_BASE}?${params.toString()}`, { headers: getAuthHeaders() });
    return res.data;
  },

  getVocabulary: async (id: string): Promise<AdminVocabularyResponse> => {
    const res = await axios.get(`${ADMIN_API_BASE}/${id}`, { headers: getAuthHeaders() });
    return res.data;
  },

  createVocabulary: async (data: AdminVocabularyRequest): Promise<AdminVocabularyResponse> => {
    const res = await axios.post(ADMIN_API_BASE, data, { headers: getAuthHeaders() });
    return res.data;
  },

  updateVocabulary: async (id: string, data: AdminVocabularyRequest): Promise<AdminVocabularyResponse> => {
    const res = await axios.put(`${ADMIN_API_BASE}/${id}`, data, { headers: getAuthHeaders() });
    return res.data;
  },

  deleteVocabulary: async (id: string): Promise<void> => {
    await axios.delete(`${ADMIN_API_BASE}/${id}`, { headers: getAuthHeaders() });
  }
};
