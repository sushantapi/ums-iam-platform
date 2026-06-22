import { useState } from "react";

export function useMutationFeedback() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();
  const [success, setSuccess] = useState<string>();

  async function run<T>(action: () => Promise<T>, successMessage: string) {
    setPending(true);
    setError(undefined);
    setSuccess(undefined);
    try {
      const result = await action();
      setSuccess(successMessage);
      return result;
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "The action could not be completed.");
      throw cause;
    } finally {
      setPending(false);
    }
  }

  return { pending, error, success, run, clear: () => { setError(undefined); setSuccess(undefined); } };
}
