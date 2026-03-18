import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { useContextCast } from "@/context/context";
import { Bell, Check, Trash2, Loader2 } from "lucide-react";
import { useEffect } from "react";

export const NotificationsDropdown = () => {
    const { notifications, unreadCount, loadingNotifications, fetchNotifications, markAsRead, deleteNotification } = useContextCast();

    useEffect(() => {
        fetchNotifications();
    }, [fetchNotifications]);

    const handleMarkAsRead = async (uuid: string) => {
        try {
            await markAsRead(uuid);
        } catch (error) {
            console.error("Errore nel marcare la notifica come letta:", error);
        }
    };

    const handleDelete = async (uuid: string) => {
        try {
            await deleteNotification(uuid);
        } catch (error) {
            console.error("Errore nell'eliminare la notifica:", error);
        }
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('it-IT', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    size="sm"
                    className="relative flex items-center gap-2"
                >
                    <Bell size={17} className="text-muted-foreground" />
                    {unreadCount > 0 && (
                        <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-red-500 text-xs text-white flex items-center justify-center font-medium">
                            {unreadCount > 99 ? '99+' : unreadCount}
                        </span>
                    )}
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-80 max-h-96 overflow-y-auto">
                <DropdownMenuLabel className="flex items-center justify-between">
                    <span>Notifiche</span>
                    {loadingNotifications && <Loader2 size={14} className="animate-spin" />}
                </DropdownMenuLabel>
                <DropdownMenuSeparator />

                {notifications.length === 0 ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">
                        Nessuna notifica
                    </div>
                ) : (
                    notifications.map((notification) => (
                        <div key={notification.uuid} className="px-2 py-2">
                            <div className="flex items-start gap-2">
                                <div className="flex-1 min-w-0">
                                    <p className={`text-sm ${notification.stato === 'READ' ? 'text-muted-foreground' : 'text-foreground'}`}>
                                        {notification.messaggio}
                                    </p>
                                    <p className="text-xs text-muted-foreground mt-1">
                                        {formatDate(notification.createdAt)}
                                    </p>
                                </div>
                                <div className="flex gap-1">
                                    {notification.stato !== 'READ' && (
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            className="h-6 w-6 p-0"
                                            onClick={() => handleMarkAsRead(notification.uuid)}
                                            title="Segna come letta"
                                        >
                                            <Check size={12} />
                                        </Button>
                                    )}
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        className="h-6 w-6 p-0 text-destructive hover:text-destructive"
                                        onClick={() => handleDelete(notification.uuid)}
                                        title="Elimina notifica"
                                    >
                                        <Trash2 size={12} />
                                    </Button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};