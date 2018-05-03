def call(config, mailSubject, message)
{
    def emailList = []
    if (config.notifications != null)
    {
        for (e in config.notifications)
        {
            if (e.get("email"))
            {
                emailList.add(e.get("email"));
            }
        }
    }
    try
    {
        for (def i = 0; i < emailList.size(); i++)
        {
            mail body: message, subject: mailSubject, to: emailList[i]
        }
    } catch (Exception ex)
    {
        echo 'there are exceptions when sending notification mail'
    }
}
