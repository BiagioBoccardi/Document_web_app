import * as React from "react";

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => {
  return (
    <textarea
      ref={ref}
      className={`border rounded-md p-2 w-full ${className}`}
      {...props}
    />
  );
});

Textarea.displayName = "Textarea";