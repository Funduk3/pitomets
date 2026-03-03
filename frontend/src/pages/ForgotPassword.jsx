import { useState } from 'react';
import { authAPI } from '../api/auth';
import { getAuthErrorMessage } from '../util/authErrors';

export const ForgotPassword = () => {
    const [email, setEmail] = useState('');
    const [sent, setSent] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await authAPI.forgotPassword(email);
            setSent(true);
        } catch (err) {
            setError(getAuthErrorMessage(err, 'forgotPassword'));
        } finally {
            setLoading(false);
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
                {error && <div className="text-red-600 text-sm">{error}</div>}
                <button
                    type="submit"
                    disabled={loading}
                    className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600 disabled:opacity-60"
                >
                    {loading ? 'Отправляем...' : 'Сбросить пароль'}
                </button>
            </form>
        </div>
    );
};
