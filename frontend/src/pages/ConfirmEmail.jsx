import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { authAPI } from '../api/auth';

export const ConfirmEmail = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            return;
        }
        authAPI.confirm(token).then(() => {
            setStatus('success');
            setTimeout(() => navigate('/login'), 3000);
        }).catch(() => {
            setStatus('error');
        });
    }, [token, navigate]);

    if (status === 'loading') return <div className="p-4">Подтверждение email...</div>;
    if (status === 'success') return <div className="p-4">Email подтвержден! Перенаправление...</div>;
    return <div className="p-4">Ошибка подтверждения. Ссылка недействительна или истекла.</div>;
};
