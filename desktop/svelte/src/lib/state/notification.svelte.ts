type NotificationOptions = {
  action?: { label: string; onClick: () => void };
  description?: string;
  duration?: number;
};

type Notification<V extends string> = {
  message: string;
  variant: V;
  id: number;
} & NotificationOptions;

type NotifyMethods<V extends string> = {
  [K in V]: (message: string, options?: NotificationOptions) => number;
};

export function createNotifications<V extends string>(variants: readonly V[]) {
  let notifications = $state<Notification<V>[]>([]);
  let _id = 0;

  function add(
    message: string,
    options?: NotificationOptions & { variant: V },
  ): number {
    const id = _id++;

    const notify = {
      id,
      message,
      duration: 5000,
      variant: options?.variant ?? variants[0],
      ...options,
    };

    notifications.push(notify);
    if (notify.duration > 0) setTimeout(() => pop(id), notify.duration);

    return id;
  }

  function pop(id: number) {
    const index = notifications.findIndex((it) => it.id === id);
    if (index !== -1) notifications.splice(index, 1);
  }

  function clearAll() {
    notifications = [];
  }

  // deriva notify.* das chaves passadas no bootstrap
  const notify = Object.fromEntries(
    variants.map((variant) => [
      variant,
      (message: string, options?: NotificationOptions) =>
        add(message, { ...options, variant }),
    ]),
  ) as NotifyMethods<V>;

  return {
    pop,
    notify,
    clearAll,
    get notifications() {
      return notifications;
    },
  };
}
