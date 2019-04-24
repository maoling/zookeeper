namespace java org.apache.zookeeper.thrift

service Greeter {

    string sayHello(1:string name);

}

struct Message {
     1: string text,
     2: string date
}

service BulletinBoard {
    void add(1: Message msg),
    list<Message> get()
}