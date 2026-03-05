const hasCyrillic = (text) => /[А-Яа-яЁё]/.test(text);

const extractBackendMessage = (error) => {
  const fromDataString = error?.response?.data;
  if (typeof fromDataString === 'string' && fromDataString.trim()) {
    return fromDataString.trim();
  }

  const fromDataMessage = error?.response?.data?.message;
  if (typeof fromDataMessage === 'string' && fromDataMessage.trim()) {
    return fromDataMessage.trim();
  }

  const fromDataError = error?.response?.data?.error;
  if (typeof fromDataError === 'string' && fromDataError.trim()) {
    return fromDataError.trim();
  }

  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim();
  }

  return '';
};

const defaultMessageByStatus = (status) => {
  switch (status) {
    case 400:
      return 'Проверьте введённые данные и попробуйте снова.';
    case 401:
      return 'Требуется авторизация. Войдите в аккаунт.';
    case 403:
      return 'Доступ запрещён.';
    case 404:
      return 'Сервис временно недоступен. Попробуйте позже.';
    case 409:
      return 'Конфликт данных. Обновите страницу и попробуйте снова.';
    case 429:
      return 'Слишком много попыток. Попробуйте позже.';
    case 500:
    case 502:
    case 503:
    case 504:
      return 'Ошибка сервера. Попробуйте позже.';
    default:
      return 'Произошла ошибка. Попробуйте ещё раз.';
  }
};

export const getAuthErrorMessage = (error, scenario = 'generic') => {
  if (!error) {
    return 'Произошла ошибка. Попробуйте ещё раз.';
  }

  if (error.code === 'ERR_NETWORK' || !error.response) {
    return 'Не удалось связаться с сервером. Проверьте интернет и попробуйте снова.';
  }

  const status = error.response?.status;
  const backendMessage = extractBackendMessage(error);
  const normalized = backendMessage.toLowerCase();

  if (scenario === 'login') {
    if (normalized.includes('invalid email or password')) {
      return 'Неверный логин или пароль';
    }
    if (normalized.includes('bad credentials')) {
      return 'Неверный логин или пароль';
    }
    if (normalized.includes('подтвердите электронную почту')) {
      return 'Подтвердите электронную почту перед входом в аккаунт.';
    }
    if (status === 400 || status === 401 || status === 403) {
      return 'Неверный логин или пароль';
    }
    if (status === 429) {
      return 'Слишком много попыток входа. Попробуйте позже.';
    }
    if (status >= 500) {
      return 'Не удалось выполнить вход. Проверьте логин и пароль и попробуйте снова.';
    }

    return 'Неверный логин или пароль';
  }

  if (scenario === 'register') {
    if (normalized.includes('user with this email already exists')) {
      return 'Пользователь с такой почтой уже зарегистрирован.';
    }
  }

  if (scenario === 'resetPassword') {
    if (normalized.includes('invalid reset token')) {
      return 'Ссылка для сброса пароля недействительна или устарела.';
    }
    if (normalized.includes('passwords do not match')) {
      return 'Пароли не совпадают.';
    }
  }

  if (scenario === 'forgotPassword') {
    if (status === 404 || normalized.includes('user not found')) {
      return 'Пользователь с такой почтой не найден.';
    }
  }

  if (normalized.includes('passwords do not match')) {
    return 'Пароли не совпадают.';
  }

  if (normalized.includes('invalid current password')) {
    return 'Текущий пароль введён неверно.';
  }

  if (normalized.includes('authentication failed')) {
    return 'Не удалось выполнить вход. Попробуйте снова.';
  }

  if (normalized.includes('access denied')) {
    return 'Недостаточно прав для этого действия.';
  }

  if (backendMessage && hasCyrillic(backendMessage)) {
    return backendMessage;
  }

  return defaultMessageByStatus(status);
};
