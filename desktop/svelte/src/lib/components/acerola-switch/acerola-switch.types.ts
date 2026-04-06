import type { Switch as SwitchPrimitive } from "bits-ui";
import type { WithoutChildrenOrChild } from "$lib/utils/cn.utils.js";

export type AcerolaSwitchProps = WithoutChildrenOrChild<SwitchPrimitive.RootProps> & {
  size?: "sm" | "default";
};
