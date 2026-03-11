import type { Hash } from "crypto";

export interface IUser {
    id: number;
    email: string;
    password: Hash;
    isAdmin: boolean;
}