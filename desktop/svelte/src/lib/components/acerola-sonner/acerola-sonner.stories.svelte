<script module>
  import AcerolaSonner from "./acerola-sonner.svelte";
  import { toast } from "svelte-sonner";
  export const meta = {
    title: "Components/AcerolaSonner",
    component: AcerolaSonner,
  };
</script>

<AcerolaSonner />

<button 
  class="bg-primary text-primary-foreground px-4 py-2 rounded"
  onclick={() => toast.success("Toast disparado!")}
>
  Disparar Toast
</button>
