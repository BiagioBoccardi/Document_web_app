import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { useContextCast } from "@/context/context";
import { Loader2 } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router";
import z from "zod";

const createUserFormSchema = z.object({
    nome: z.string().min(2, "Il nome è obbligatorio"),
    email: z.string().email("Email non valida"),
    passwordHash: z
      .string()
      .min(8, "La password deve essere lunga almeno 8 caratteri")
      .regex(/[a-zA-Z]/, "La password deve contenere almeno una lettera")
      .regex(/\d/, "La password deve contenere almeno un numero"),
    confirmPassword: z
      .string()
      .min(8, "La password di conferma deve essere lunga almeno 8 caratteri"),
    isAdmin: z.boolean(),
  })
  .refine((data) => data.passwordHash === data.confirmPassword, {
    message: "Le password non coincidono",
    path: ["confirmPassword"],
  });

export function SignupForm() {
  const [isAdmin, setIsAdmin] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);
  const { createUser } = useContextCast();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const formData = new FormData(form);

    const data = {
      nome: formData.get("nome") as string,
      email: formData.get("email") as string,
      passwordHash: formData.get("passwordHash") as string,
      confirmPassword: formData.get("confirmPassword") as string,
      isAdmin,
    };

    const parsed = createUserFormSchema.safeParse(data);

    if (!parsed.success) {
      const fieldErrors: Record<string, string> = {};
      parsed.error.issues.forEach((issue) => {
        fieldErrors[issue.path[0] as string] = issue.message;
      });
      setErrors(fieldErrors);
      return;
    }

    setErrors({});
    setIsLoading(true)

    try {
      const payload = {
        nome: parsed.data.nome,
        email: parsed.data.email,
        passwordHash: parsed.data.passwordHash,
        isAdmin: parsed.data.isAdmin
      };

      await createUser(payload);
      form.reset();
      setIsAdmin(false);
    } catch (error) {
      console.error("Errore durante la creazione:", error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
      <div className="w-full max-w-sm">
        <Card>
          <CardHeader>
            <CardTitle>Crea un'account</CardTitle>
            <CardDescription>
              Inserisci le tue informazioni qui sotto per creare il tuo account
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit}>
              <FieldGroup>
                <Field className="grid gap-1.5">
                  <FieldLabel htmlFor="nome">Nome</FieldLabel>
                  <Input
                    name="nome"
                    id="nome"
                    type="text"
                    placeholder="John Doe"
                    aria-invalid={!!errors.nome}
                  />
                  {errors.nome && (
                    <p className="text-red-500 text-sm">{errors.nome}</p>
                  )}
                </Field>
                <Field className="grid gap-1.5">
                  <FieldLabel htmlFor="email">Email</FieldLabel>
                  <Input
                    name="email"
                    id="email"
                    type="email"
                    placeholder="m@example.com"
                    aria-invalid={!!errors.email}
                  />
                  <FieldDescription>
                    L'indirizzo email deve essere valido.
                  </FieldDescription>
                  {errors.email && (
                    <p className="text-red-500 text-sm">{errors.email}</p>
                  )}
                </Field>
                <Field className="grid gap-1.5">
                  <FieldLabel htmlFor="passwordHash">Password</FieldLabel>
                  <Input
                    id="passwordHash"
                    name="passwordHash"
                    type="password"
                    aria-invalid={!!errors.password}
                  />
                  <FieldDescription>
                    Deve essere lunga almeno 8 caratteri e contenere almeno un
                    numero e una lettera.
                  </FieldDescription>
                  {errors.password && (
                    <p className="text-red-500 text-sm">{errors.password}</p>
                  )}
                </Field>
                <Field className="grid gap-1.5">
                  <FieldLabel htmlFor="confirmPassword">
                    Conferma Password
                  </FieldLabel>
                  <Input
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    aria-invalid={!!errors.confirmPassword}
                  />
                  <FieldDescription>Conferma la tua password.</FieldDescription>
                  {errors.confirmPassword && (
                    <p className="text-red-500 text-sm">
                      {errors.confirmPassword}
                    </p>
                  )}
                </Field>
                <FieldGroup>
                  <Field>
                    <Button type="submit" disabled={isLoading}>
                      {isLoading ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Creazione in corso...
                        </>
                      ) : (
                        "Crea Account"
                      )}
                    </Button>
                    <FieldDescription className="text-center">
                      Hai già un account?
                      <Link
                        to="/signin"
                        className="text-blue-500 hover:underline"
                      >
                        Accedi
                      </Link>
                    </FieldDescription>
                  </Field>
                </FieldGroup>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
