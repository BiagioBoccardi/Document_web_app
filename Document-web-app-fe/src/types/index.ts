
export interface IUser {
    id: number;
    nome: string;
    email: string;
    passwordHash: string;
    isAdmin: boolean;
}