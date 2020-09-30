from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import FTPServer
from pyftpdlib.authorizers import DummyAuthorizer

authorizer = DummyAuthorizer()
authorizer.add_user('admin', 'password', './target_folder', perm='elradfmwM')
handler = FTPHandler
handler.authorizer = authorizer

server = FTPServer(('0.0.0.0', 8888), handler)
server.serve_forever()