
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
    members: IUser[];
}

export interface INotification {
  uuid: string;
  messaggio: string;
  userId: number;
  stato: 'PENDING' | 'SENT' | 'READ' | 'FAILED';
  createdAt: string;
  readAt?: string;
}

export interface IDocument {
    id: string;
    userId: number;
    filename: string;
    content: string;
    uploadDate: string;
    lastModified: string;
    metadata: {
        size: number;
        mimeType: string;
        checksum: string;
        fileType: string;
    };
}