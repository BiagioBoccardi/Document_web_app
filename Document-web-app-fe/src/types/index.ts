
export interface IUser {
    id: number;
    nome: string;
    email: string;
    passwordHash: string;
    isAdmin: boolean;
}

export interface IGruppo {
    id: number;
    name: string;
    owner: IUser;
    membri: IUser[];
}

export interface INotification {
  uuid: string;
  messaggio: string;
  userId: number;
  stato: 'PENDING' | 'SENT' | 'READ' | 'FAILED';
  createdAt: string;
  readAt?: string;
}