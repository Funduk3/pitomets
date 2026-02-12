import { useState } from 'react';
import { authAPI } from '../api/auth';

export const ForgotPassword = () => {
    const [email, setEmail] = useState('');
    const [sent, setSent] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await authAPI.forgotPassword(email);
            setSent(true);
        } catch (e) {
            console.error(e);
            alert('Ошибка запроса. Возможно, пользователь не найден.');
        }
    };

    if (sent) return <div className="p-4">На вашу почту отправлена ссылка для сброса пароля.</div>;

    return (
        <div className="p-4 max-w-md mx-auto">
            <h2 className="text-xl mb-4">Восстановление пароля</h2>
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <input
                    type="email"
                    placeholder="Email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    required
                    className="border p-2 rounded"
                />
                <button type="submit" className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600">
                    Сбросить пароль
                </button>
            </form>
        </div>
    );
};
