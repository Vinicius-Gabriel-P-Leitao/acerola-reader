import { redirect } from "@sveltejs/kit";

// NOTE: Redirect para /home como default
export const load = () => redirect(302, "/home");
