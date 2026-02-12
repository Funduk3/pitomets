import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { authAPI } from '../api/auth';

export const ResetPassword = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        if (password !== confirmPassword) {
            setError('Пароли не совпадают');
            return;
        }
        try {
            await authAPI.resetPassword(token, password, confirmPassword);
            alert('Пароль успешно изменен');
            navigate('/login');
        } catch (e) {
            console.error(e);
            alert('Ошибка сброса пароля');
        }
    };

    if (!token) return <div className="p-4">Неверная ссылка</div>;

    return (
       <div className="p-4 max-w-md mx-auto">
            <h2 className="text-xl mb-4">Новый пароль</h2>
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <input
                    type="password"
                    placeholder="Новый пароль"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    required
                    className="border p-2 rounded"
                />
                <input
                    type="password"
                    placeholder="Повторите пароль"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    required
                    className="border p-2 rounded"
                />
                {error && <div className="text-red-600 text-sm">{error}</div>}
                <button type="submit" className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600">
                    Сохранить
                </button>
            </form>
        </div>
    );
};
