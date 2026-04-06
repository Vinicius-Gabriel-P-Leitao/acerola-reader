export interface AcerolaSelectOption {
  value: string;
  label: string;
}

export type AcerolaSelectProps = {
  value?: string;
  class?: string;
  placeholder?: string;
  options: AcerolaSelectOption[];
};
