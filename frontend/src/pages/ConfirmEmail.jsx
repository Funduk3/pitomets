import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { authAPI } from '../api/auth';
import { userAPI } from '../api/user';
import { useAuth } from '../context/AuthContext';

export const ConfirmEmail = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading');
    const { setUser } = useAuth();

    useEffect(() => {
        if (!token) {
            setStatus('error');
            return;
        }

        const confirmAndLogin = async () => {
            try {
                const response = await authAPI.confirm(token);
                localStorage.setItem('accessToken', response.accessToken);
                localStorage.setItem('refreshToken', response.refreshToken);
                const profile = await userAPI.getCurrentProfile();
                setUser(profile);
                setStatus('success');
                setTimeout(() => navigate('/', { replace: true }), 1200);
            } catch {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                setUser(null);
                setStatus('error');
            }
        };

        confirmAndLogin();
    }, [token, navigate, setUser]);

    if (status === 'loading') return <div className="p-4">Подтверждение email...</div>;
    if (status === 'success') return <div className="p-4">Email подтвержден! Выполняем вход...</div>;
    return <div className="p-4">Ошибка подтверждения. Ссылка недействительна или истекла.</div>;
};
